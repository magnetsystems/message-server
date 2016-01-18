package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for all administrative needs
 * Created by mmicevic on 1/13/16.
 */
@Path("admin")
public class AdminResource {

    private static Logger LOGGER = Logger.getLogger(AdminResource.class);
    private static final String PARAM_LOG_NAME = "name";
    private static final String PARAM_LOG_LEVEL = "level";

    @GET
    @Path("loglevel")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogLevel() {

        LOGGER.trace("TRACE getLogLevel");
        LOGGER.debug("DEBUG getLogLevel");
        LOGGER.info("INFO getLogLevel");
        LOGGER.warn("WARN getLogLevel");
        LOGGER.error("ERROR getLogLevel");
        LOGGER.fatal("FATAL getLogLevel");
        return getCurrentLogLevelResponse();
    }

    @PUT
    @Path("loglevel/{" + PARAM_LOG_LEVEL + "}/{" + PARAM_LOG_NAME + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeLogLevel(@PathParam(PARAM_LOG_LEVEL) String newLogLevel,
                                   @PathParam(PARAM_LOG_NAME) String logName) {

        Level l = getLevelForName(newLogLevel);
        if (l == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("level=" + newLogLevel).build();
        }
        setLogLevel(l, logName);
        return getCurrentLogLevelResponse();
    }

    private static Level getLevelForName(String levelName) {

        Level l = null;
        if (Level.TRACE.toString().equalsIgnoreCase(levelName)) {
            l = Level.TRACE;
        } else if (Level.DEBUG.toString().equalsIgnoreCase(levelName)) {
            l = Level.DEBUG;
        } else if (Level.INFO.toString().equalsIgnoreCase(levelName)) {
            l = Level.INFO;
        } else if (Level.WARN.toString().equalsIgnoreCase(levelName)) {
            l = Level.WARN;
        } else if (Level.ERROR.toString().equalsIgnoreCase(levelName)) {
            l = Level.ERROR;
        } else if (Level.FATAL.toString().equalsIgnoreCase(levelName)) {
            l = Level.FATAL;
        } else if (Level.OFF.toString().equalsIgnoreCase(levelName)) {
            l = Level.OFF;
        }
        return l;
    }


    private static Response getCurrentLogLevelResponse() {
        return Response.status(Response.Status.OK).entity(getCurrentLogLevel()).build();
    }
    private  static Map<String, String> getCurrentLogLevel() {

        Map<String, String> levels = new HashMap<String, String>();
        Enumeration c = LogManager.getLoggerRepository().getCurrentCategories();

//        while (c.hasMoreElements()) {
//            Category logger = (Category) c.nextElement();
//            boolean hasParent = true;
//            while(hasParent) {
//                Category p = logger.getParent();
//                if (p != null) {
//                    logger = p;
//                } else {
//                    hasParent = false;
//                }
//            }
//            levels.put(logger.getName(), getLevelName(logger.getEffectiveLevel()));
//        }

//        while (c.hasMoreElements()) {
//            Category logger = (Category) c.nextElement();
//            levels.put(logger.getParent().getName(), getLevelName(logger.getParent().getEffectiveLevel()));
//        }

        Enumeration e = LogManager.getCurrentLoggers();
        while (e.hasMoreElements()) {
            Logger logger = (Logger) e.nextElement();
            levels.put(logger.getParent().getName(), getLevelName(logger.getParent().getEffectiveLevel()));
        }
        return levels;
    }

    private static String getLevelName(Level level) {
        return level == null ? null : level.toString();
    }

//    private static void setLogLevel(Level level) {
//
//        Enumeration e = LogManager.getCurrentLoggers();
//        while (e.hasMoreElements()) {
//            Logger logger = (Logger) e.nextElement();
//            logger.setLevel(level);
//        }
//    }
    private static void setLogLevel(Level level, String logName) {

        Logger logger = LogManager.getLogger(logName);
        logger.setLevel(level);
    }
}
