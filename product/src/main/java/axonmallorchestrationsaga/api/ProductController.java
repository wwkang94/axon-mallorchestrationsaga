package axonmallorchestrationsaga.api;

import axonmallorchestrationsaga.aggregate.*;
import axonmallorchestrationsaga.command.*;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

//<<< Clean Arch / Inbound Adaptor
@RestController
public class ProductController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public ProductController(
        CommandGateway commandGateway,
        QueryGateway queryGateway
    ) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @RequestMapping(
        value = "/products/{id}/decreasestock",
        method = RequestMethod.PUT,
        produces = "application/json;charset=UTF-8"
    )
    public CompletableFuture decreaseStock(
        @PathVariable("id") String id,
        @RequestBody DecreaseStockCommand decreaseStockCommand
    ) throws Exception {
        System.out.println("##### /product/decreaseStock  called #####");

        decreaseStockCommand.setProductId(id);
        // send command
        return commandGateway.send(decreaseStockCommand);
    }

    @RequestMapping(value = "/products", method = RequestMethod.POST)
    public CompletableFuture register(
        @RequestBody RegisterCommand registerCommand
    ) throws Exception {
        System.out.println("##### /product/register  called #####");

        // send command
        return commandGateway
            .send(registerCommand)
            .thenApply(id -> {
                ProductAggregate resource = new ProductAggregate();
                BeanUtils.copyProperties(registerCommand, resource);

                resource.setProductId((String) id);

                return new ResponseEntity<>(hateoas(resource), HttpStatus.OK);
            });
    }

    @RequestMapping(
        value = "/products/{id}/increasestock",
        method = RequestMethod.PUT,
        produces = "application/json;charset=UTF-8"
    )
    public CompletableFuture increaseStock(
        @PathVariable("id") String id,
        @RequestBody IncreaseStockCommand increaseStockCommand
    ) throws Exception {
        System.out.println("##### /product/increaseStock  called #####");

        increaseStockCommand.setProductId(id);
        // send command
        return commandGateway.send(increaseStockCommand);
    }

    @Autowired
    EventStore eventStore;

    @GetMapping(value = "/products/{id}/events")
    public ResponseEntity getEvents(@PathVariable("id") String id) {
        ArrayList resources = new ArrayList<ProductAggregate>();
        eventStore.readEvents(id).asStream().forEach(resources::add);

        CollectionModel<ProductAggregate> model = CollectionModel.of(resources);

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    EntityModel<ProductAggregate> hateoas(ProductAggregate resource) {
        EntityModel<ProductAggregate> model = EntityModel.of(resource);

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
}
//>>> Clean Arch / Inbound Adaptor
