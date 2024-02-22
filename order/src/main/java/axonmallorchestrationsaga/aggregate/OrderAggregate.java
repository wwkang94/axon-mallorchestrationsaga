package axonmallorchestrationsaga.aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

import axonmallorchestrationsaga.command.*;
import axonmallorchestrationsaga.event.*;
import axonmallorchestrationsaga.query.*;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

//<<< DDD / Aggregate Root
@Aggregate
@Data
@ToString
public class OrderAggregate {

    @AggregateIdentifier
    private String orderId;

    private String productName;
    private String productId;
    private String status;
    private Integer qty;
    private String userId;

    public OrderAggregate() {}

    @CommandHandler
    public OrderAggregate(OrderCommand command) {
        OrderPlacedEvent event = new OrderPlacedEvent();
        BeanUtils.copyProperties(command, event);

        //TODO: check key generation is properly done
        if (event.getOrderId() == null) event.setOrderId(createUUID());

        apply(event);
    }

    @CommandHandler
    public void handle(UpdateStatusCommand command) {
        OrderCompletedEvent event = new OrderCompletedEvent();
        BeanUtils.copyProperties(command, event);

        apply(event);
    }

    @CommandHandler
    public void handle(OrderCancelCommand command) {
        OrderCancelledEvent event = new OrderCancelledEvent();
        BeanUtils.copyProperties(command, event);

        apply(event);
    }

    //<<< Etc / ID Generation
    private String createUUID() {
        return UUID.randomUUID().toString();
    }

    //>>> Etc / ID Generation

    //<<< EDA / Event Sourcing

    @EventSourcingHandler
    public void on(OrderPlacedEvent event) {
        BeanUtils.copyProperties(event, this);
        //TODO: business logic here

    }

    @EventSourcingHandler
    public void on(OrderCompletedEvent event) {
        //TODO: business logic here

    }

    @EventSourcingHandler
    public void on(OrderCancelledEvent event) {
        //TODO: business logic here

    }
    //>>> EDA / Event Sourcing

}
//>>> DDD / Aggregate Root
