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
public class DeliveryController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public DeliveryController(
        CommandGateway commandGateway,
        QueryGateway queryGateway
    ) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @RequestMapping(value = "/deliveries", method = RequestMethod.POST)
    public CompletableFuture startDelivery(
        @RequestBody StartDeliveryCommand startDeliveryCommand
    ) throws Exception {
        System.out.println("##### /delivery/startDelivery  called #####");

        // send command
        return commandGateway
            .send(startDeliveryCommand)
            .thenApply(id -> {
                DeliveryAggregate resource = new DeliveryAggregate();
                BeanUtils.copyProperties(startDeliveryCommand, resource);

                resource.setDeliveryId((String) id);

                return new ResponseEntity<>(hateoas(resource), HttpStatus.OK);
            });
    }

    @RequestMapping(
        value = "/deliveries/{id}/canceldelivery",
        method = RequestMethod.PUT,
        produces = "application/json;charset=UTF-8"
    )
    public CompletableFuture cancelDelivery(@PathVariable("id") String id)
        throws Exception {
        System.out.println("##### /delivery/cancelDelivery  called #####");
        CancelDeliveryCommand cancelDeliveryCommand = new CancelDeliveryCommand();
        cancelDeliveryCommand.setDeliveryId(id);
        // send command
        return commandGateway.send(cancelDeliveryCommand);
    }

    @Autowired
    EventStore eventStore;

    @GetMapping(value = "/deliveries/{id}/events")
    public ResponseEntity getEvents(@PathVariable("id") String id) {
        ArrayList resources = new ArrayList<DeliveryAggregate>();
        eventStore.readEvents(id).asStream().forEach(resources::add);

        CollectionModel<DeliveryAggregate> model = CollectionModel.of(
            resources
        );

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    EntityModel<DeliveryAggregate> hateoas(DeliveryAggregate resource) {
        EntityModel<DeliveryAggregate> model = EntityModel.of(resource);

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
}
//>>> Clean Arch / Inbound Adaptor
