package com.example.grpc.service.course;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {

        final Server server = ServerBuilder
            .forPort(50005)
            .addService(new SumServiceImpl())
            .build();

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Got shutdown request");
                server.shutdown();
            }
        }));

        server.awaitTermination();
    }
}