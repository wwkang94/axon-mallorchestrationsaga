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
public class DeliveryListQueryController {

    private final QueryGateway queryGateway;

    //<<< Etc / RSocket
    private final ReactorQueryGateway reactorQueryGateway;

    //>>> Etc / RSocket

    public DeliveryListQueryController(
        QueryGateway queryGateway,
        ReactorQueryGateway reactorQueryGateway
    ) {
        this.queryGateway = queryGateway;
        this.reactorQueryGateway = reactorQueryGateway;
    }

    @GetMapping("/deliveries")
    public CompletableFuture findAll(DeliveryListQuery query) {
        return queryGateway
            .query(
                query,
                ResponseTypes.multipleInstancesOf(DeliveryReadModel.class)
            )
            .thenApply(resources -> {
                List modelList = new ArrayList<EntityModel<DeliveryReadModel>>();

                resources
                    .stream()
                    .forEach(resource -> {
                        modelList.add(hateoas(resource));
                    });

                CollectionModel<DeliveryReadModel> model = CollectionModel.of(
                    modelList
                );

                return new ResponseEntity<>(model, HttpStatus.OK);
            });
    }

    @GetMapping("/deliveries/{id}")
    public CompletableFuture findById(@PathVariable("id") String id) {
        DeliveryListSingleQuery query = new DeliveryListSingleQuery();
        query.setDeliveryId(id);

        return queryGateway
            .query(
                query,
                ResponseTypes.optionalInstanceOf(DeliveryReadModel.class)
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

    EntityModel<DeliveryReadModel> hateoas(DeliveryReadModel resource) {
        EntityModel<DeliveryReadModel> model = EntityModel.of(resource);

        model.add(
            Link.of("/deliveries/" + resource.getDeliveryId()).withSelfRel()
        );

        model.add(
            Link
                .of(
                    "/deliveries/" +
                    resource.getDeliveryId() +
                    "/canceldelivery"
                )
                .withRel("canceldelivery")
        );

        model.add(
            Link
                .of("/deliveries/" + resource.getDeliveryId() + "/events")
                .withRel("events")
        );

        return model;
    }

    //<<< Etc / RSocket
    @MessageMapping("deliveries.all")
    public Flux<DeliveryReadModel> subscribeAll() {
        return reactorQueryGateway.subscriptionQueryMany(
            new DeliveryListQuery(),
            DeliveryReadModel.class
        );
    }

    @MessageMapping("deliveries.{id}.get")
    public Flux<DeliveryReadModel> subscribeSingle(
        @DestinationVariable String id
    ) {
        DeliveryListSingleQuery query = new DeliveryListSingleQuery();
        query.setDeliveryId(id);

        return reactorQueryGateway.subscriptionQuery(
            query,
            DeliveryReadModel.class
        );
    }
    //>>> Etc / RSocket

}
