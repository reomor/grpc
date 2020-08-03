package com.example.grpc.service.greet;

import com.example.grpc.greet.GreetEveryoneRequest;
import com.example.grpc.greet.GreetEveryoneResponse;
import com.example.grpc.greet.GreetManyTimesRequest;
import com.example.grpc.greet.GreetRequest;
import com.example.grpc.greet.GreetResponse;
import com.example.grpc.greet.GreetServiceGrpc;
import com.example.grpc.greet.Greeting;
import com.example.grpc.greet.LongGreetRequest;
import com.example.grpc.greet.LongGreetResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
            .usePlaintext() // deactivate ssl
            .build();

//        doUnaryCall(channel);
//        doServerStreamingCall(channel);
//        doClientStreamingCall(channel);
        doBiDirectionalStreamingCall(channel);
        channel.shutdown()
            .awaitTermination(5, TimeUnit.SECONDS);
    }

    private void doUnaryCall(ManagedChannel channel) {
        GreetServiceGrpc.GreetServiceBlockingStub syncClient = GreetServiceGrpc.newBlockingStub(channel);
        // Unary
        GreetResponse response = syncClient.greet(GreetRequest.newBuilder()
            .setGreeting(
                Greeting.newBuilder()
                    .setFirstName("Ololo")
                    .setLastName("Rush")
                    .build()
            ).build()
        );
        log.info(response.getResult());
    }

    private void doServerStreamingCall(ManagedChannel channel) {
        GreetServiceGrpc.GreetServiceBlockingStub syncClient = GreetServiceGrpc.newBlockingStub(channel);
        // Server streaming
        syncClient.greetManyTimes(
            GreetManyTimesRequest.newBuilder()
                .setGreeting(Greeting.newBuilder()
                    .setFirstName("Ololo")
                    .setLastName("Loloev")
                    .build()
                ).build()
        ).forEachRemaining(
            greetManyTimesResponse -> log.info(greetManyTimesResponse.getResult())
        );
    }

    private void doClientStreamingCall(ManagedChannel channel) {

        GreetServiceGrpc.GreetServiceStub asyncClient = GreetServiceGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<LongGreetRequest> requestObserver = asyncClient.longGreet(new StreamObserver<LongGreetResponse>() {
            @Override
            public void onNext(LongGreetResponse value) {
                log.info(value.getResult());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.error("Server has completed work");
                latch.countDown();
            }
        });

        requestObserver.onNext(
            LongGreetRequest.newBuilder()
                .setGreeting(Greeting.newBuilder()
                    .setFirstName("Lolo1")
                    .setLastName("Ololoev")
                    .build()
                ).build()
        );

        requestObserver.onNext(
            LongGreetRequest.newBuilder()
                .setGreeting(Greeting.newBuilder()
                    .setFirstName("Lolo2")
                    .setLastName("Ololoev")
                    .build()
                ).build()
        );

        requestObserver.onNext(
            LongGreetRequest.newBuilder()
                .setGreeting(Greeting.newBuilder()
                    .setFirstName("Lolo3")
                    .setLastName("Ololoev")
                    .build()
                ).build()
        );

        requestObserver.onCompleted();

        try {
            latch.await(3L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private void doBiDirectionalStreamingCall(ManagedChannel channel) throws InterruptedException {
        GreetServiceGrpc.GreetServiceStub asyncClient = GreetServiceGrpc.newStub(channel);

        CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<GreetEveryoneRequest> requestObserver = asyncClient.greetEveryone(new StreamObserver<GreetEveryoneResponse>() {
            @Override
            public void onNext(GreetEveryoneResponse value) {
                log.info(value.getResult());
            }

            @Override
            public void onError(Throwable t) {
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Server has completed work");
                finishLatch.countDown();
            }
        });

        Arrays.asList("First", "Second", "Third", "Last")
            .forEach(name -> requestObserver.onNext(
                GreetEveryoneRequest.newBuilder()
                    .setGreeting(Greeting.newBuilder()
                        .setFirstName(name)
                        .setLastName("LastName")
                        .build())
                    .build()
                )
            );

        requestObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            log.error("Exceed");
        }
    }
}
