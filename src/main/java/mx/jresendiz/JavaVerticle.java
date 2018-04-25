package mx.jresendiz;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jresendiz on 24/04/18.
 */
public class JavaVerticle extends AbstractVerticle {
    private Logger log = LoggerFactory.getLogger(JavaVerticle.class);

    public void start() {
        log.info("Java Verticle started ...");
    }
}
