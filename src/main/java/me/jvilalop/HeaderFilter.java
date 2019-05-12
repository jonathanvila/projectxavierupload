package me.jvilalop;

import org.apache.camel.Exchange;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("noFilter")
public class HeaderFilter implements HeaderFilterStrategy {

    @Override
    public boolean applyFilterToCamelHeaders(String arg0, Object arg1, Exchange arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean applyFilterToExternalHeaders(String arg0, Object arg1, Exchange arg2) {
        // TODO Auto-generated method stub
        return false;
    }

}