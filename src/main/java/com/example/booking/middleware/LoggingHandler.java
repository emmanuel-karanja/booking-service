package com.example.booking.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHandler implements Handler<RoutingContext>{
    public static final Logger logger=LoggerFactory.getLogger(LoggingHandler.class);

    @Override
    public void handle(RoutingContext ctx){

        long start=System.currentTimeMillis();

        // Do on body end.
        ctx.addBodyEndHandler(v->{

            long duration=System.currentTimeMillis()-start;

            logger.info(
                    "{} {} {} {}ms",
                    ctx.request().method(),
                    ctx.request().path(),
                    ctx.response().getStatusCode(),
                    duration);
        });

        ctx.next();

    }
}