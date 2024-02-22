package axonmallorchestrationsaga.query;

import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;

@Entity
@Table(name = "Order_table")
@Data
@Relation(collectionRelation = "orders")
//<<< EDA / Read Model

public class OrderReadModel {

    @Id
    private String orderId;

    private String productName;

    private String productId;

    private String status;

    private Integer qty;

    private String userId;
}
//>>> EDA / Read Model
