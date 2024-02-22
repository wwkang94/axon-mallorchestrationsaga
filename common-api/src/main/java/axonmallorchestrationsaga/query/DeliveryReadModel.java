package axonmallorchestrationsaga.query;

import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;

@Entity
@Table(name = "Delivery_table")
@Data
@Relation(collectionRelation = "deliveries")
//<<< EDA / Read Model

public class DeliveryReadModel {

    @Id
    private String deliveryId;

    private String userId;

    private String address;

    private String orderId;

    private String productId;

    private Integer qty;

    private String status;
}
//>>> EDA / Read Model
