package com.example.grpc.service.greet;

import com.example.grpc.greet.GreetEveryoneRequest;
import com.example.grpc.greet.GreetEveryoneResponse;
import com.example.grpc.greet.GreetManyTimesRequest;
import com.example.grpc.greet.GreetManyTimesResponse;
import com.example.grpc.greet.GreetRequest;
import com.example.grpc.greet.GreetResponse;
import com.example.grpc.greet.GreetServiceGrpc;
import com.example.grpc.greet.Greeting;
import com.example.grpc.greet.LongGreetRequest;
import com.example.grpc.greet.LongGreetResponse;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class GreetServiceImpl extends GreetServiceGrpc.GreetServiceImplBase {

    // Unary
    @Override
    public void greet(GreetRequest request, StreamObserver<GreetResponse> responseObserver) {
        Greeting greeting = request.getGreeting();

        String result = "Hello " + greeting.getFirstName();
        GreetResponse response = GreetResponse.newBuilder()
            .setResult(result)
            .build();

        // send response
        responseObserver.onNext(response);

        // complete the RPC call
        responseObserver.onCompleted();
    }

    // Server streaming
    @Override
    public void greetManyTimes(GreetManyTimesRequest request, StreamObserver<GreetManyTimesResponse> responseObserver) {

        try {
            for (int i = 0; i < 10; i++) {
                responseObserver.onNext(GreetManyTimesResponse.newBuilder()
                    .setResult("Hello " + request.getGreeting().getFirstName() + " " + i)
                    .build()
                );
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            responseObserver.onCompleted();
        }
    }

    // Client streaming
    @Override
    public StreamObserver<LongGreetRequest> longGreet(StreamObserver<LongGreetResponse> responseObserver) {
        return new StreamObserver<LongGreetRequest>() {

            String result = "";

            @Override
            public void onNext(LongGreetRequest value) {
                result += ". Hello " + value.getGreeting().getFirstName() + "! ";
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(
                    LongGreetResponse.newBuilder()
                    .setResult(result)
                    .build()
                );
                responseObserver.onCompleted();
            }
        };
    }

    // BiDirectional streaming
    @Override
    public StreamObserver<GreetEveryoneRequest> greetEveryone(StreamObserver<GreetEveryoneResponse> responseObserver) {
        return new StreamObserver<GreetEveryoneRequest>() {
            @Override
            public void onNext(GreetEveryoneRequest value) {
                responseObserver.onNext(GreetEveryoneResponse.newBuilder()
                    .setResult("Hello " + value.getGreeting().getFirstName())
                    .build());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
