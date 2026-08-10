package com.example.booking.middleware;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ValidationException;
import com.example.booking.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ErrorHandler implements Handler<RoutingContext>{

    private static final Logger _logger=LoggerFactory.getLogger(ErrorHandler.class);
    @Override
    public void handle(RoutingContext ctx) {

        Throwable error = ctx.failure();

        _logger.error("Request failed", error);

        // _logger.error(error.getClass().getName());
        if (error instanceof ValidationException e) {
            ctx.response()
                    .setStatusCode(400)
                    .end(json(error));
        } else if (error instanceof NotFoundException e) {
            ctx.response()
                    .setStatusCode(404)
                    .end(json(error));
            // This is a serious oversight that has to be rectified.
        } else if (error instanceof ReplyException replyException) {
            ctx.response()
                    .setStatusCode(replyException.failureCode())
                    .end(json(replyException));
        }
        else if (error instanceof IllegalArgumentException) {

            ctx.response()
                    .setStatusCode(400)
                    .end(json(error));
        }else {
            _logger.error(error.getMessage());
            ctx.response()
                    .setStatusCode(500)
                    .end(json(error));

        }
    }

    private String json(Throwable error){
        return new JsonObject()
                .put("error", error == null ? "Unknown error" : error.getMessage())
                .encode();  // Convert to Json string.
    }
}