package me.jvilalop;

import org.apache.camel.Attachment;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;

/**
 * A Camel Java8 DSL Router
 */
@Component
public class MyRouteBuilder extends RouteBuilder {

    public void configure() {
        getContext().setTracing(false);

        restConfiguration().component("servlet").contextPath("/");

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
                .to("file:./upload");
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