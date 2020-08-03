package com.example.grpc.service.course;

import com.example.AverageRequest;
import com.example.AverageResponse;
import com.example.DeadlineRequest;
import com.example.DeadlineResponse;
import com.example.DecompositionRequest;
import com.example.DecompositionResponse;
import com.example.MaximumRequest;
import com.example.MaximumResponse;
import com.example.SumRequest;
import com.example.SumResponse;
import com.example.SumServiceGrpc;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

public class SumServiceImpl extends SumServiceGrpc.SumServiceImplBase {

    @Override
    public void sum(SumRequest request, StreamObserver<SumResponse> responseObserver) {

        responseObserver.onNext(SumResponse.newBuilder()
            .setResult(
                request.getA() + request.getB()
            ).build());

        responseObserver.onCompleted();
    }

    @Override
    public void decompose(DecompositionRequest request, StreamObserver<DecompositionResponse> responseObserver) {
        int number = request.getNumber();

        int factor = 2;
        while (number > 1) {
            if (number % factor == 0) {
                responseObserver.onNext(
                    DecompositionResponse.newBuilder()
                        .setFactor(factor)
                        .build()
                );
                number /= factor;
            } else {
                factor += 1;
            }
        }

        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<AverageRequest> average(final StreamObserver<AverageResponse> responseObserver) {
        return new StreamObserver<AverageRequest>() {

            int sum = 0;
            int amount = 0;

            @Override
            public void onNext(AverageRequest value) {
                System.out.println(value.getNumber());
                sum += value.getNumber();
                amount++;
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(AverageResponse.newBuilder()
                    .setResult((double) sum / amount)
                    .build()
                );
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<MaximumRequest> maximum(final StreamObserver<MaximumResponse> responseObserver) {
        return new StreamObserver<MaximumRequest>() {
            private int max = Integer.MIN_VALUE;

            @Override
            public void onNext(MaximumRequest maximumRequest) {
                if (maximumRequest.getNumber() > max) {
                    max = maximumRequest.getNumber();
                }
                responseObserver.onNext(MaximumResponse.newBuilder()
                    .setMaximum(max)
                    .build()
                );
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void withDeadline(DeadlineRequest request, StreamObserver<DeadlineResponse> responseObserver) {
        Context context = Context.current();
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            return;
        }

        if (context.isCancelled()) {
            return;
        }

        responseObserver.onNext(DeadlineResponse.newBuilder()
            .setResult(request.getNumber() * 2)
            .build()
        );

        responseObserver.onCompleted();
    }
}
