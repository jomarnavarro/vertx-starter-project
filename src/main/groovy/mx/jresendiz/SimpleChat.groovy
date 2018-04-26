package mx.jresendiz

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

@Slf4j
class SimpleChat extends AbstractVerticle {

    public void start() {
        // This method is called whenever a verticle starts
        log.info("Verticle has started!!")
    }

    public void stop() {
        // This method is called whenever a verticle is closed or dies
        log.info("Verticle has been closed!")
    }
}
