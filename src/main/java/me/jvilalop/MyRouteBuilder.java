package me.jvilalop;

import com.sun.mail.iap.ByteArray;
import org.apache.camel.Attachment;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * A Camel Java8 DSL Router
 */
@Component
public class MyRouteBuilder extends RouteBuilder {

    public void configure() {
        getContext().setTracing(true);

        restConfiguration()
                .component("servlet")
                .contextPath("/")
                .port(8090);

        rest()
                .post("/upload")
                    .id("uploadAction")
                    .consumes("multipart/form-data")
                    .produces("")
                    .to("direct:upload")
                .get("/health")
                    .to("direct:health");

        from("direct:upload")
                .unmarshal(new CustomizedMultipartDataFormat())
                .split()
                    .attachments()
                    .process(processMultipart())
                    .log("Processing PART ------> ${date:now:HH:mm:ss.SSS} [${header.CamelFileName}] // [${header.part_contenttype}] // [${header.part_name}]]")
                    .choice()
                        .when(isZippedFile())
                            .split(new ZipSplitter())
                            .streaming()
                            .log(".....ZIP File processed : ${header.CamelFileName}")
                            .to("direct:store")
                        .endChoice()
                        .otherwise()
                            .to("direct:store");
        
        from("direct:store")
                .convertBodyTo(String.class)
                .to("file:./upload")
                .to("direct:insights");
        
        from("direct:insights")
                .process(exchange -> {
                    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                    multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    multipartEntityBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);
                    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                    String file = exchange.getIn().getBody(String.class);
                    multipartEntityBuilder.addPart("upload", new ByteArrayBody(file.getBytes(), ContentType.create("application/vnd.redhat.testareno.something+json"), filename));
                    exchange.getOut().setBody(multipartEntityBuilder.build());
                })
                .setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
                .setHeader("x-rh-identity", constant(getRHIdentity()))
                .setHeader("x-rh-insights-request-id", constant(getRHInsightsRequestId()))
                .to("http4://localhost:8080/api/ingress/v1/upload")
        .log("respuesta ${body}")
        .end();
        
        /*
        from("kafka:platform.upload.testareno")
                .log("message received");
         */
    }

    private String getRHInsightsRequestId() {
        // 52df9f748eabcfea
        return UUID.randomUUID().toString();
    }

    private String getRHIdentity() {
        // '{"identity": {"account_number": "12345", "internal": {"org_id": "54321"}}}'
        return RHIdentity.builder()
                .accountNumber("12345")
                .internalOrgId("54321")
                .build().toHash();
    }

    private Predicate isZippedFile() {
        return exchange -> "application/zip".equalsIgnoreCase(exchange.getMessage().getHeader("part_contenttype").toString());
    }

    private Processor processMultipart() {
        return exchange -> {
            DataHandler dataHandler = exchange.getIn().getBody(Attachment.class).getDataHandler();
            exchange.getIn().setHeader(Exchange.FILE_NAME, dataHandler.getName());
            exchange.getIn().setHeader("part_contenttype", dataHandler.getContentType());
            exchange.getIn().setHeader("part_name", dataHandler.getName());
            exchange.getIn().setBody(dataHandler.getInputStream());
        };
    }


}