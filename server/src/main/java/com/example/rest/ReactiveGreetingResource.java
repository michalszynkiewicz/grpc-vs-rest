package com.example.rest;

import io.smallrye.common.annotation.Blocking;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;

@Path("/hello")
@ApplicationScoped
public class ReactiveGreetingResource {
    private static final Logger log = Logger.getLogger(ReactiveGreetingResource.class);

    final RestDto response;

    public ReactiveGreetingResource() {
        response = new RestDto();
        ArrayList<RestRecord> records = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            RestRecord record = new RestRecord();
            record.comment = "com-ment";
            record.message = RandomStringUtils.randomAlphabetic(100);
            records.add(record);
        }
        response.records = records;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    public RestDto hello(RestDto restDto) {
        return response;
    }
}