package me.jvilalop.dataformat;

import me.jvilalop.model.InputDataModel;
import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;

import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.camel.builder.SimpleBuilder.simple;

public class ReportModelDataFormat {
    
    public void transform(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        InputDataModel dataModel = InputDataModel.builder().customerId("CID9876") //exchange.getMessage().getHeader("customerid").toString())
                .filename(simple("${CamelFileName}").getText())
                .numberOfHosts(Long.parseLong(exchange.getMessage().getHeader("numberofhosts").toString()))
                .totalDiskSpace(Long.parseLong(exchange.getMessage().getHeader("totaldiskspace").toString()))
                .build();

        exchange.getOut().setBody(dataModel);
    }

}
