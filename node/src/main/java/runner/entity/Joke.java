package runner.entity;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jokes")
public class Joke {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String text;
    private String creationDate;
    private String changeDate;
    private Integer Rating;

}