package com.example.grpc;

import com.example.grpcbench.MutinyConsumerGrpc;
import com.example.grpcbench.Record;
import com.example.grpcbench.RecordList;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@io.quarkus.grpc.GrpcService
public class GrpcService extends MutinyConsumerGrpc.ConsumerImplBase {
    final RecordList response;

    GrpcService() {
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            records.add(Record.newBuilder().setMessage(RandomStringUtils.randomAlphabetic(100)).setComment("com-ment").build());
        }
        response = RecordList.newBuilder().addAllRecords(records).setName("response").build();
    }

    @Override
    public Uni<RecordList> consume(RecordList request) {
        return Uni.createFrom().item(response);
    }

    @Override
    public Multi<RecordList> consumeStream(Multi<RecordList> requestList) {
        CompletableFuture<MultiEmitter<? super RecordList>> emitter = new CompletableFuture<>();

        Multi<RecordList> result = Multi.createFrom().<RecordList>emitter(emitter::complete, BackPressureStrategy.BUFFER);
        emitter.thenAccept(
                multiEmitter ->
                        requestList
                                .onCompletion().invoke(multiEmitter::complete)
                                .subscribe().with(recordList -> {
                                    multiEmitter.emit(response);
                                }
                        )
        );
        return result;
    }
}
