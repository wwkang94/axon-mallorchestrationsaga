package axonmallorchestrationsaga.query;

import axonmallorchestrationsaga.aggregate.*;
import axonmallorchestrationsaga.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ProcessingGroup("deliveryList")
public class DeliveryListCQRSHandlerReusingAggregate {

    //<<< EDA / CQRS
    @Autowired
    private DeliveryReadModelRepository repository;

    //<<< Etc / RSocket
    @Autowired
    private QueryUpdateEmitter queryUpdateEmitter;

    //>>> Etc / RSocket

    @QueryHandler
    public List<DeliveryReadModel> handle(DeliveryListQuery query) {
        return repository.findAll();
    }

    @QueryHandler
    public Optional<DeliveryReadModel> handle(DeliveryListSingleQuery query) {
        return repository.findById(query.getDeliveryId());
    }

    @EventHandler
    public void whenDeliveryStarted_then_CREATE(DeliveryStartedEvent event)
        throws Exception {
        DeliveryReadModel entity = new DeliveryReadModel();
        DeliveryAggregate aggregate = new DeliveryAggregate();
        aggregate.on(event);

        BeanUtils.copyProperties(aggregate, entity);

        repository.save(entity);

        //<<< Etc / RSocket
        queryUpdateEmitter.emit(DeliveryListQuery.class, query -> true, entity);
        //>>> Etc / RSocket

    }

    @EventHandler
    public void whenDeliveryCancelled_then_UPDATE(DeliveryCancelledEvent event)
        throws Exception {
        repository
            .findById(event.getDeliveryId())
            .ifPresent(entity -> {
                DeliveryAggregate aggregate = new DeliveryAggregate();

                BeanUtils.copyProperties(entity, aggregate);
                aggregate.on(event);
                BeanUtils.copyProperties(aggregate, entity);

                repository.save(entity);

                //<<< Etc / RSocket
                queryUpdateEmitter.emit(
                    DeliveryListSingleQuery.class,
                    query ->
                        query.getDeliveryId().equals(event.getDeliveryId()),
                    entity
                );
                //>>> Etc / RSocket

            });
    }
    //>>> EDA / CQRS
}
