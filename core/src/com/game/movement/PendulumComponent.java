package com.game.movement;

import com.game.Component;
import com.game.utils.interfaces.UpdatableConsumer;
import com.game.utils.objects.Pendulum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendulumComponent extends Component {

    private Pendulum pendulum;
    private UpdatableConsumer<Pendulum> updatableConsumer;

    public PendulumComponent(Pendulum pendulum) {
        setPendulum(pendulum);
    }

}
