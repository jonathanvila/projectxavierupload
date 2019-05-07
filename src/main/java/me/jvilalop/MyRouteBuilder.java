package me.jvilalop;

import org.apache.camel.Attachment;
import org.apache.camel.Exchange;
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
        getContext().setTracing(true);

        restConfiguration().component("servlet").contextPath("/");

        rest()
                .post("/upload")
                    .id("uploadAction")
                    .consumes("multipart/form-data")
                    .to("direct:unzipFile_customized")
                .get("/health")
                    .to("direct:health");

        from("direct:unzipFile")
                .unmarshal()
                .mimeMultipart()
                .split()
                .attachments()
                .process(processMultipart())
                .log("Processing ZIP ------> [${header.CamelFileName}] // [${header.part_type}] // [${header.part_name}] // [${header.part_value}]")
                .split(new ZipSplitter())
                .log("File processed : ${header.CamelFileName}")
                .transform().simple("simple: out");

        from("direct:unzipFile_customized")
                .unmarshal(new CustomizedMultipartDataFormat())
                .split()
                .attachments()
                .process(processMultipart())
                .log("Processing ZIP------> [${header.CamelFileName}] // [${header.part_type}] // [${header.part_name}] // [${header.part_value}]")
                .split(new ZipSplitter())
                .log("File processed : ${header.CamelFileName}")
                .transform().simple("simple: out");
    }

    private Processor processMultipart() {
        return exchange -> {
            DataHandler dataHandler = exchange.getIn().getBody(Attachment.class).getDataHandler();
            exchange.getIn().setHeader(Exchange.FILE_NAME, dataHandler.getName());
            exchange.getIn().setHeader("part_type", dataHandler.getContentType());
            exchange.getIn().setHeader("part_name", dataHandler.getName());
            exchange.getIn().setHeader("part_value", (!dataHandler.getContentType().equals("application/zip")) ? dataHandler.getContent() : "XXX FILE XXX");
            exchange.getIn().setBody(dataHandler.getInputStream());
        };
    }


}