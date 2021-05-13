package com.example.rest;

import com.example.grpcbench.MutinyConsumerGrpc;
import com.example.grpcbench.Record;
import com.example.grpcbench.RecordList;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

@Path("/test")
@Singleton
public class TestResource {

    public static final int RECORD_SIZE = 100;
    public static final int RECORD_COUNT = 1000;
    public static final int TEST_SIZE = 100000;
    final RestDto restRequest;

    final RecordList grpcRequest;

    @GrpcClient("grpc-client")
    MutinyConsumerGrpc.MutinyConsumerStub grpcClient;

    TestResource() {
        restRequest = new RestDto();
        ArrayList<RestRecord> records = new ArrayList<>();
        for (int i = 0; i < RECORD_COUNT; i++) {
            RestRecord record = new RestRecord();
            record.comment = "com-ment";
            record.message = RandomStringUtils.randomAlphabetic(RECORD_SIZE);
            records.add(record);
        }
        restRequest.records = records;

        List<Record> grpcRecords = new ArrayList<>();
        for (int i = 0; i < RECORD_COUNT; i++) {
            grpcRecords.add(Record.newBuilder().setMessage(RandomStringUtils.randomAlphabetic(RECORD_SIZE)).setComment("com-ment").build());
        }
        grpcRequest = RecordList.newBuilder().addAllRecords(grpcRecords).setName("response").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("/rest")
    public Results testRest() {
        Results results = new Results();

        // warm-up
        System.out.println("starting rest warm-up");
        testRest(TEST_SIZE);
        long start = System.currentTimeMillis();
        System.out.println("starting rest test");
        testRest(TEST_SIZE);
        results.restTime = System.currentTimeMillis() - start;

        System.out.println("done");

        return results;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    @Path("/grpc")
    public Results testGrpc() {
        Results results = new Results();

        // warm-up
        System.out.println("starting grpc warm-up");
        testGrpc(TEST_SIZE);
        System.out.println("starting grpc test");
        long start = System.currentTimeMillis();
        testGrpc(TEST_SIZE);
        results.grpcTime = System.currentTimeMillis() - start;


        System.out.println("done");

        return results;
    }

    private void testRest(int calls) {
        ClientImpl client = (ClientImpl) ClientBuilderImpl.newBuilder().build();
        CompletionStageRxInvoker rx = client.target("http://localhost:8080/hello")
                .request(MediaType.APPLICATION_JSON).rx();
        CountDownLatch countDown = new CountDownLatch(calls);
        for (int j = 0; j < calls; j++) {
            rx.post(Entity.entity(restRequest, MediaType.APPLICATION_JSON_TYPE), RestDto.class)
                    .thenRun(countDown::countDown).exceptionally(error -> {
                error.printStackTrace();
                return null;
            });
        }
        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed waiting for all grpc calls to be finished");
        }
    }

    private void testGrpc(int calls) {
        CountDownLatch countDown = new CountDownLatch(calls);
        for (int j = 0; j < calls; j++) {
            Uni<RecordList> response = grpcClient.consume(grpcRequest);
            response.subscribe().with(result -> {
                if (result.getRecordsCount() != 10) {
                    System.out.println("Wrong result count!");
                }
                countDown.countDown();
            });
        }
        try {
            countDown.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed waiting for all grpc calls to be finished");
        }
    }


    public static class Results {
        Long grpcTime;
        Long restTime;

        public Long getGrpcTime() {
            return grpcTime;
        }

        public void setGrpcTime(Long grpcTime) {
            this.grpcTime = grpcTime;
        }

        public Long getRestTime() {
            return restTime;
        }

        public void setRestTime(Long restTime) {
            this.restTime = restTime;
        }
    }

    @Path("/hello")
    public interface RestClient {
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        CompletionStage<RestDto> request(RestDto restDto);
    }
}