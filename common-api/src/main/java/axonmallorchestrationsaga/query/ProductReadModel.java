package axonmallorchestrationsaga.query;

import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;

@Entity
@Table(name = "Product_table")
@Data
@Relation(collectionRelation = "products")
//<<< EDA / Read Model

public class ProductReadModel {

    @Id
    private String productId;

    private String productName;

    private Integer stock;

    private String orderId;
}
//>>> EDA / Read Model
