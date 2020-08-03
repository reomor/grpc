package com.example.grpc.service.greet;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {

        Server server = ServerBuilder
            .forPort(8080)
            .addService(new GreetServiceImpl())
            .build();

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Got shutdown request");
                server.shutdown();
            })
        );

        server.awaitTermination();
    }
}