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
public class ProductListQueryController {

    private final QueryGateway queryGateway;

    //<<< Etc / RSocket
    private final ReactorQueryGateway reactorQueryGateway;

    //>>> Etc / RSocket

    public ProductListQueryController(
        QueryGateway queryGateway,
        ReactorQueryGateway reactorQueryGateway
    ) {
        this.queryGateway = queryGateway;
        this.reactorQueryGateway = reactorQueryGateway;
    }

    @GetMapping("/products")
    public CompletableFuture findAll(ProductListQuery query) {
        return queryGateway
            .query(
                query,
                ResponseTypes.multipleInstancesOf(ProductReadModel.class)
            )
            .thenApply(resources -> {
                List modelList = new ArrayList<EntityModel<ProductReadModel>>();

                resources
                    .stream()
                    .forEach(resource -> {
                        modelList.add(hateoas(resource));
                    });

                CollectionModel<ProductReadModel> model = CollectionModel.of(
                    modelList
                );

                return new ResponseEntity<>(model, HttpStatus.OK);
            });
    }

    @GetMapping("/products/{id}")
    public CompletableFuture findById(@PathVariable("id") String id) {
        ProductListSingleQuery query = new ProductListSingleQuery();
        query.setProductId(id);

        return queryGateway
            .query(
                query,
                ResponseTypes.optionalInstanceOf(ProductReadModel.class)
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

    EntityModel<ProductReadModel> hateoas(ProductReadModel resource) {
        EntityModel<ProductReadModel> model = EntityModel.of(resource);

        model.add(
            Link.of("/products/" + resource.getProductId()).withSelfRel()
        );

        model.add(
            Link
                .of("/products/" + resource.getProductId() + "/decreasestock")
                .withRel("decreasestock")
        );
        model.add(
            Link
                .of("/products/" + resource.getProductId() + "/increasestock")
                .withRel("increasestock")
        );

        model.add(
            Link
                .of("/products/" + resource.getProductId() + "/events")
                .withRel("events")
        );

        return model;
    }

    //<<< Etc / RSocket
    @MessageMapping("products.all")
    public Flux<ProductReadModel> subscribeAll() {
        return reactorQueryGateway.subscriptionQueryMany(
            new ProductListQuery(),
            ProductReadModel.class
        );
    }

    @MessageMapping("products.{id}.get")
    public Flux<ProductReadModel> subscribeSingle(
        @DestinationVariable String id
    ) {
        ProductListSingleQuery query = new ProductListSingleQuery();
        query.setProductId(id);

        return reactorQueryGateway.subscriptionQuery(
            query,
            ProductReadModel.class
        );
    }
    //>>> Etc / RSocket

}
