package axonmallorchestrationsaga.api;

import axonmallorchestrationsaga.query.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class OrderStatusQueryController {

    private final QueryGateway queryGateway;

    //<<< Etc / RSocket
    private final ReactorQueryGateway reactorQueryGateway;

    //>>> Etc / RSocket

    public OrderStatusQueryController(
        QueryGateway queryGateway,
        ReactorQueryGateway reactorQueryGateway
    ) {
        this.queryGateway = queryGateway;
        this.reactorQueryGateway = reactorQueryGateway;
    }

    @GetMapping("/orders")
    public CompletableFuture findAll(OrderStatusQuery query) {
        return queryGateway
            .query(
                query,
                ResponseTypes.multipleInstancesOf(OrderReadModel.class)
            )
            .thenApply(resources -> {
                List modelList = new ArrayList<EntityModel<OrderReadModel>>();

                resources
                    .stream()
                    .forEach(resource -> {
                        modelList.add(hateoas(resource));
                    });

                CollectionModel<OrderReadModel> model = CollectionModel.of(
                    modelList
                );

                return new ResponseEntity<>(model, HttpStatus.OK);
            });
    }

    @GetMapping("/orders/{id}")
    public CompletableFuture findById(@PathVariable("id") String id) {
        OrderStatusSingleQuery query = new OrderStatusSingleQuery();
        query.setOrderId(id);

        return queryGateway
            .query(
                query,
                ResponseTypes.optionalInstanceOf(OrderReadModel.class)
            )
            .thenApply(resource -> {
                if (!resource.isPresent()) {
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                }

                return new ResponseEntity<>(
                    hateoas(resource.get()),
                    HttpStatus.OK
                );
            })
            .exceptionally(ex -> {
                throw new RuntimeException(ex);
            });
    }

    EntityModel<OrderReadModel> hateoas(OrderReadModel resource) {
        EntityModel<OrderReadModel> model = EntityModel.of(resource);

        model.add(Link.of("/orders/" + resource.getOrderId()).withSelfRel());

        model.add(
            Link
                .of("/orders/" + resource.getOrderId() + "/updatestatus")
                .withRel("updatestatus")
        );
        model.add(
            Link
                .of("/orders/" + resource.getOrderId() + "/ordercancel")
                .withRel("ordercancel")
        );

        model.add(
            Link
                .of("/orders/" + resource.getOrderId() + "/events")
                .withRel("events")
        );

        return model;
    }

    //<<< Etc / RSocket
    @MessageMapping("orders.all")
    public Flux<OrderReadModel> subscribeAll() {
        return reactorQueryGateway.subscriptionQueryMany(
            new OrderStatusQuery(),
            OrderReadModel.class
        );
    }

    @MessageMapping("orders.{id}.get")
    public Flux<OrderReadModel> subscribeSingle(
        @DestinationVariable String id
    ) {
        OrderStatusSingleQuery query = new OrderStatusSingleQuery();
        query.setOrderId(id);

        return reactorQueryGateway.subscriptionQuery(
            query,
            OrderReadModel.class
        );
    }
    //>>> Etc / RSocket

}
