package com.example.devVerPrac;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.io.*;
public class MainVerticle extends AbstractVerticle {
    String encodedImage = "";
    HandlebarsTemplateEngine engine;
    @Override
    public void start() throws Exception {
        HttpServer server = vertx.createHttpServer();
        engine=HandlebarsTemplateEngine.create();
        MongoClient client = MongoClient.createShared(vertx,new JsonObject().put("host","127.0.0.1").put("port",27017).put("db_name","mongodevdb"));

        /* server.requestHandler(request -> {

            // This handler gets called for each request that arrives on the server
            HttpServerResponse response = request.response();
            response.putHeader("content-type", "text/plain");

            // Write to the response and end it
            response.end("Hello dev!");
        });*/

        /*

        Router router = Router.router(vertx);

        router.route().handler(routingContext -> {

            // This handler will be called for every request
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");

            // Write to the response and end it
            response.end("dev here!");
        });*/

       // server.requestHandler(router::accept).listen(8080);

        //server.requestHandler(router::accept).listen(8080);

        Router router=Router.router(vertx);
        router.route().handler(BodyHandler.create());


        router.get("/register").handler(routingContext->{

            engine.render(routingContext, "templates/register.hbs", res -> {
                if (res.succeeded()) {
                    routingContext.response().end(res.result());
                } else {
                    routingContext.fail(res.cause());
                }
            });
        });

        server.requestHandler(router::accept).listen(8080);
        System.out.print("Runing on 8080");


        router.get("/login").handler(routingContext->{

            engine.render(routingContext, "templates/Login.hbs", res -> {
                routingContext.response().setChunked(true);
                if (res.succeeded()) {
                    routingContext.response().end(res.result());

                } else {
                    routingContext.fail(res.cause());
                }
            });
        });



        router.post("/saveMongoDB").handler(cnt->{
            for (FileUpload fu : cnt.fileUploads()) {

                if (fu.fileName().contains("jpg") || fu.fileName().contains("png") || fu.fileName().contains("jpeg")) {
                    try {
                        File f = new File(fu.uploadedFileName());
                        BufferedImage image = ImageIO.read(f);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", baos);
                        byte[] res = baos.toByteArray();
                        encodedImage = Base64.encode(baos.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            JsonObject js=new JsonObject()
                .put("name",cnt.request().getFormAttribute("name"))
                .put("phone",cnt.request().getFormAttribute("phone"))
                .put("email",cnt.request().getFormAttribute("email"))
                .put("question",cnt.request().getFormAttribute("question"))
                .put("Image",encodedImage);
            client.insert("users",js,hnd->{
                if(hnd.succeeded())
                {
                    client.find("users",new JsonObject(),event->{
                       cnt.put("data",event.result());
                       engine.render(cnt,"templates/allUsers.hbs",hand->{
                           if(hand.succeeded()){
                               cnt.response().end(hand.result());
                           }

                       });

                    });
                }
            });
        });



        router.get("/allUsers").handler(routingContext->{
           client.find("users",new JsonObject(),event -> {
              routingContext.put("data",event.result());
              engine.render(routingContext,"templates/allUsers.hbs",hand->{
                  if(hand.succeeded()){
                      routingContext.response().end(hand.result());
                  }else{
                      routingContext.fail(hand.cause());
                  }

              });
           });

        });
        router.post("/deleteUser").handler(routingContext->{
            client.findOneAndDelete("users",new JsonObject().put("_id",routingContext.request().params().get("id")),handler->{
                routingContext.response().end();
            });
        });

        router.post("/updateUser").handler(rtx->{
            String id=rtx.request().params().get("_id");
            JsonObject config=new JsonObject();

            JsonObject update=new JsonObject().put("$set",new JsonObject()
                .put("email",rtx.request().params().get("email"))
                .put("phone",rtx.request().params().get("phone"))
                .put("name",rtx.request().params().get("name"))
                .put("question",rtx.request().params().get("question")));


            client.updateCollection("users",new JsonObject().put("_id",rtx.request().getParam("_id")),update,hnd->{
                if(hnd.succeeded()){
                    System.out.print("Success");
                }else{
                    System.out.print(hnd.cause());
                }

            });
            rtx.response().end();

        });
    }
}
