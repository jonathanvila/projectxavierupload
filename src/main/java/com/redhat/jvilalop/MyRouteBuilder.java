package com.redhat.jvilalop;

import org.apache.camel.Attachment;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.model.dataformat.MimeMultipartDataFormat;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import java.util.Iterator;

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
                    .to("direct:unzipFile")
                .get("/health")
                    .to("direct:health");

        from("direct:unzipFile")
                .unmarshal()
                .mimeMultipart()
                .split()
                .attachments()
                .process(exchange -> {
                    DataHandler dataHandler = exchange.getIn().getBody(Attachment.class).getDataHandler();
                    exchange.getIn().setHeader(Exchange.FILE_NAME, dataHandler.getName());
                    exchange.getIn().setHeader("tipo", dataHandler.getContentType());
                    exchange.getIn().setHeader("nombre", dataHandler.getName());
                    exchange.getIn().setHeader("value", (!dataHandler.getContentType().equals("application/zip")) ? dataHandler.getContent() : "XXX FILE XXX");
                    exchange.getIn().setBody(dataHandler.getInputStream());
                })
                .log("Procesando ------> [${header.CamelFileName}] // [${header.tipo}] // [${header.nombre}] // [${header.value}]")
                .split(new ZipSplitter())
                .log("Fichero procesado : ${header.CamelFileName}")
                .transform().simple("simple: SALIDA");
    }


}