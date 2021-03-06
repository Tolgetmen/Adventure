package net.overmy.adventure.ashley.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import net.overmy.adventure.ashley.MyMapper;
import net.overmy.adventure.ashley.components.AnimationComponent;
import net.overmy.adventure.ashley.components.MyRotationComponent;
import net.overmy.adventure.ashley.components.OutOfCameraComponent;

/*
      Created by Andrey Mikheev on 30.09.2017
      Contact me → http://vk.com/id17317
 */

public class MyRotationSystem extends IteratingSystem {


    @SuppressWarnings( "unchecked" )
    public MyRotationSystem () {
        super( Family.all( MyRotationComponent.class ).
                exclude( OutOfCameraComponent.class ).get() );
    }


    @Override
    protected void processEntity ( Entity entity, float delta ) {
        if ( !MyMapper.PHYSICAL.has( entity ) ) {
            return;
        }

        final btRigidBody btRigidBody = MyMapper.PHYSICAL.get( entity ).body;
        final Matrix4 transform = btRigidBody.getWorldTransform();
        transform.rotate( Vector3.Y, 5.0f );
        btRigidBody.setWorldTransform( transform );

        if ( MyMapper.ANIMATION.has( entity ) ) {
            AnimationComponent animationComponent = MyMapper.ANIMATION.get( entity );
            animationComponent.queue( 0, 1.0f );
        }
    }
}
