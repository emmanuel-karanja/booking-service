package com.example.booking.verticles;

import com.example.booking.config.AppConfig;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MainVerticle extends AbstractVerticle {

    private final static Logger _logger= LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject()
                        .put("path", "application.json"));

        // 2. Add store to Retriever options
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore);

        // 3. Create a retriever and get config.
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig()
                .onSuccess(json -> {
                    System.out.println(json.encodePrettily());
                    _logger.info("Configs loaded {}", json.encodePrettily());
                })
                // Convert JSON to AppConfig
                .map(json -> json.mapTo(AppConfig.class))
                // Deploy DatabaseVerticle
                .compose(config -> {
                    DeploymentOptions dbOptions =
                            new DeploymentOptions()
                                    .setConfig(JsonObject.mapFrom(config.database()));
                    return vertx.deployVerticle(
                            new DatabaseVerticle(),
                            dbOptions
                    ).map(config);  // Note, this is critical!
                })
                // Deploy HttpVerticle
                .compose(config -> {
                    DeploymentOptions httpOptions =
                            new DeploymentOptions()
                                    .setConfig(JsonObject.mapFrom(config));
                    return vertx.deployVerticle(
                            new HttpVerticle(),
                            httpOptions
                    );
                })
                .onSuccess(id -> {
                    System.out.println("Application started.");
                    _logger.info("Application Started successully {}", id);
                    startPromise.complete();
                })
                .onFailure(error->{
                       _logger.error("Application startup failed",error);
                        startPromise.fail(error);
                });
    }
}