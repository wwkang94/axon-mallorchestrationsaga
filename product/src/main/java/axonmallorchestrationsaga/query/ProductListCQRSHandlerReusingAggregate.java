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
@ProcessingGroup("productList")
public class ProductListCQRSHandlerReusingAggregate {

    //<<< EDA / CQRS
    @Autowired
    private ProductReadModelRepository repository;

    //<<< Etc / RSocket
    @Autowired
    private QueryUpdateEmitter queryUpdateEmitter;

    //>>> Etc / RSocket

    @QueryHandler
    public List<ProductReadModel> handle(ProductListQuery query) {
        return repository.findAll();
    }

    @QueryHandler
    public Optional<ProductReadModel> handle(ProductListSingleQuery query) {
        return repository.findById(query.getProductId());
    }

    @EventHandler
    public void whenStockDecreased_then_UPDATE(StockDecreasedEvent event)
        throws Exception {
        repository
            .findById(event.getProductId())
            .ifPresent(entity -> {
                ProductAggregate aggregate = new ProductAggregate();

                BeanUtils.copyProperties(entity, aggregate);
                aggregate.on(event);
                BeanUtils.copyProperties(aggregate, entity);

                repository.save(entity);

                //<<< Etc / RSocket
                queryUpdateEmitter.emit(
                    ProductListSingleQuery.class,
                    query -> query.getProductId().equals(event.getProductId()),
                    entity
                );
                //>>> Etc / RSocket

            });
    }

    @EventHandler
    public void whenProductRegistered_then_CREATE(ProductRegisteredEvent event)
        throws Exception {
        ProductReadModel entity = new ProductReadModel();
        ProductAggregate aggregate = new ProductAggregate();
        aggregate.on(event);

        BeanUtils.copyProperties(aggregate, entity);

        repository.save(entity);

        //<<< Etc / RSocket
        queryUpdateEmitter.emit(ProductListQuery.class, query -> true, entity);
        //>>> Etc / RSocket

    }

    @EventHandler
    public void whenStockIncreased_then_UPDATE(StockIncreasedEvent event)
        throws Exception {
        repository
            .findById(event.getProductId())
            .ifPresent(entity -> {
                ProductAggregate aggregate = new ProductAggregate();

                BeanUtils.copyProperties(entity, aggregate);
                aggregate.on(event);
                BeanUtils.copyProperties(aggregate, entity);

                repository.save(entity);

                //<<< Etc / RSocket
                queryUpdateEmitter.emit(
                    ProductListSingleQuery.class,
                    query -> query.getProductId().equals(event.getProductId()),
                    entity
                );
                //>>> Etc / RSocket

            });
    }
    //>>> EDA / CQRS
}
