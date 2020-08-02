package com.example.grpc.service.course;

import com.example.DeadlineRequest;
import com.example.DeadlineResponse;
import com.example.MaximumRequest;
import com.example.MaximumResponse;
import com.example.SumServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

    public static void main(String[] args) throws InterruptedException {
        GrpcClient client = new GrpcClient();
        client.run();
    }

    private void run() throws InterruptedException {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50005)
            .usePlaintext() // deactivate ssl
            .build();

        SumServiceGrpc.SumServiceStub asyncClient = SumServiceGrpc.newStub(channel);
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<MaximumRequest> requestObserver = asyncClient.maximum(new StreamObserver<MaximumResponse>() {
            @Override
            public void onNext(MaximumResponse response) {
                System.out.println(response.getMaximum());
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
                requestObserver.onNext(MaximumRequest.newBuilder().setNumber(integer).build());
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            System.out.println("Exceed");
        }

        //
        SumServiceGrpc.SumServiceBlockingStub syncClient = SumServiceGrpc.newBlockingStub(channel);
        try {
            DeadlineResponse response = syncClient.withDeadlineAfter(1000, TimeUnit.MILLISECONDS)
                .withDeadline(DeadlineRequest.newBuilder()
                    .setNumber(333)
                    .build()
                );
            System.out.println(response.getResult());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                System.out.println("Deadline exceed");
            }
        }

        channel.shutdown()
            .awaitTermination(5, TimeUnit.SECONDS);
    }
}
