package com.example.grpc.service.course;

import com.example.DeadlineRequest;
import com.example.DeadlineResponse;
import com.example.MaximumRequest;
import com.example.MaximumResponse;
import com.example.SumServiceGrpc;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

public class SumServiceImpl extends SumServiceGrpc.SumServiceImplBase {
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
