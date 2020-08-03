package com.example.grpc.service.course;

import com.example.AverageRequest;
import com.example.AverageResponse;
import com.example.DeadlineRequest;
import com.example.DeadlineResponse;
import com.example.DecompositionRequest;
import com.example.MaximumRequest;
import com.example.MaximumResponse;
import com.example.SumRequest;
import com.example.SumResponse;
import com.example.SumServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcClient.class);

    public static void main(String[] args) throws InterruptedException {
        GrpcClient client = new GrpcClient();
        client.run();
    }

    private void run() throws InterruptedException {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50005)
            .usePlaintext() // deactivate ssl
            .build();

        SumServiceGrpc.SumServiceBlockingStub syncClient = SumServiceGrpc.newBlockingStub(channel);
        SumServiceGrpc.SumServiceStub asyncClient = SumServiceGrpc.newStub(channel);

        // Unary
        SumResponse response = syncClient.sum(
            SumRequest.newBuilder()
                .setA(10)
                .setB(3)
                .build()
        );

        log.info("Sum is: {}", response.getResult());

        // Server streaming
        syncClient.decompose(
            DecompositionRequest.newBuilder()
                .setNumber(123456789)
                .build()
        ).forEachRemaining(decompositionResponse -> log.info("Factor is: {}", decompositionResponse.getFactor()));

        // Client streaming
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<AverageRequest> averageRequestStreamObserver = asyncClient.average(new StreamObserver<AverageResponse>() {
            @Override
            public void onNext(AverageResponse value) {
                log.info("Average is: {}", value.getResult());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                log.info("Server complete it's work");
                latch.countDown();
            }
        });

        for (int i = 0; i < 10000; i++) {
            averageRequestStreamObserver.onNext(
                AverageRequest.newBuilder()
                    .setNumber(i + 1)
                    .build()
            );
        }

        averageRequestStreamObserver.onCompleted();

        try {
            latch.await(3L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

        // Bidirectional
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<MaximumRequest> maximumRequestStreamObserver = asyncClient.maximum(new StreamObserver<MaximumResponse>() {
            @Override
            public void onNext(MaximumResponse response) {
                log.info("Maximum is: {}", response.getMaximum());
            }

            @Override
            public void onError(Throwable throwable) {
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        });

        try {
            for (Integer integer : Arrays.asList(1, 2, 3, 4, 3, 2, 1, 6)) {
                maximumRequestStreamObserver.onNext(MaximumRequest.newBuilder().setNumber(integer).build());
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            maximumRequestStreamObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        maximumRequestStreamObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            log.error("Exceed");
        }

        try {
            DeadlineResponse deadlineResponse = syncClient.withDeadlineAfter(1000, TimeUnit.MILLISECONDS)
                .withDeadline(DeadlineRequest.newBuilder()
                    .setNumber(333)
                    .build()
                );
            log.info("Deadline result: {}", deadlineResponse.getResult());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                log.error("Deadline exceed");
            }
        }

        channel.shutdown()
            .awaitTermination(5, TimeUnit.SECONDS);
    }
}
