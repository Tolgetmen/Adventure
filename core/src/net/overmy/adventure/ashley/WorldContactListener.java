package net.overmy.adventure.ashley;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;

import net.overmy.adventure.AshleyWorld;
import net.overmy.adventure.DEBUG;
import net.overmy.adventure.MyPlayer;
import net.overmy.adventure.ashley.components.COMP_TYPE;
import net.overmy.adventure.ashley.components.OutOfCameraComponent;
import net.overmy.adventure.ashley.components.PhysicalComponent;
import net.overmy.adventure.ashley.components.PositionComponent;
import net.overmy.adventure.ashley.components.RemoveByTimeComponent;
import net.overmy.adventure.ashley.components.RemoveByLevelComponent;
import net.overmy.adventure.logic.DynamicLevels;

/*
      Created by Andrey Mikheev on 30.09.2017
      Contact me → http://vk.com/id17317
 */

public class WorldContactListener extends ContactListener {

    private ImmutableArray< Entity > entities = null;

    private StringBuilder stringBuilder = null;


    public WorldContactListener () {
        super();
        stringBuilder = new StringBuilder();
    }


    @Override
    public void onContactProcessed ( int userValue1, boolean m1, int userValue2, boolean m2 ) {
        if ( DEBUG.CONTACTS.get() ) {
            stringBuilder.setLength( 0 );
            stringBuilder.append( userValue1 );
            stringBuilder.append( m1 ? " (match) " : " (not match)" );
            stringBuilder.append( " -> " );
            stringBuilder.append( userValue2 );
            stringBuilder.append( m2 ? " (match) " : " (not match)" );
            Gdx.app.debug( "onContactProcessed", stringBuilder.toString() );
        }

        // Начинаем поиск тех двух Entity, у которых userValue физических тела
        // совпадают с теми, что пришли в onContactProcessed
        Entity entity1 = null;
        Entity entity2 = null;

        for ( Entity entity : entities ) {
            PhysicalComponent comp = MyMapper.PHYSICAL.get( entity );

            if ( comp.body.getUserValue() == userValue1 ) {
                entity1 = entity;
            }
            if ( comp.body.getUserValue() == userValue2 ) {
                entity2 = entity;
            }
        }

        // Одну из Entity не нашли, вероятно, что сейчас удаляется физическое тело
        if ( entity1 == null || entity2 == null ) {
            return;
        }

        // У Entity пропал физический компонент, пока искали совпадения?
        if ( !MyMapper.PHYSICAL.has( entity1 ) || !MyMapper.PHYSICAL.has( entity2 ) ) {
            return;
        }

        startContactEntities( entity1, entity2 );
    }


    @Override
    public void onContactEnded ( int userValue1, boolean m1, int userValue2, boolean m2 ) {
        if ( DEBUG.CONTACTS.get() ) {
            stringBuilder.setLength( 0 );
            stringBuilder.append( userValue1 );
            stringBuilder.append( m1 ? " (match) " : " (not match)" );
            stringBuilder.append( " -> " );
            stringBuilder.append( userValue2 );
            stringBuilder.append( m2 ? " (match) " : " (not match)" );
            Gdx.app.debug( "onContactEnded", stringBuilder.toString() );
        }

        // Опять поиск тех двух Entity, у которых userValue физических тела
        // совпадают с теми, что пришли в onContactEnded
        Entity entity1 = null;
        Entity entity2 = null;

        for ( Entity entity : entities ) {
            PhysicalComponent comp = MyMapper.PHYSICAL.get( entity );

            if ( comp.body.getUserValue() == userValue1 ) {
                entity1 = entity;
            }
            if ( comp.body.getUserValue() == userValue2 ) {
                entity2 = entity;
            }
        }

        if ( entity1 == null || entity2 == null ) {
            return;
        }

        if ( !MyMapper.PHYSICAL.has( entity1 ) || !MyMapper.PHYSICAL.has( entity2 ) ) {
            return;
        }

        endContactEntities( entity1, entity2 );
    }


    private void startContactEntities ( Entity entity01, Entity entity02 ) {
        COMP_TYPE type1 = MyMapper.TYPE.get( entity01 ).type;
        COMP_TYPE type2 = MyMapper.TYPE.get( entity02 ).type;

        if ( DEBUG.CONTACTS.get() ) {
            stringBuilder.setLength( 0 );
            stringBuilder.append( type1 );
            stringBuilder.append( " -> " );
            stringBuilder.append( type2 );
            Gdx.app.debug( "startContact", stringBuilder.toString() );
        }

        boolean contact1Player = type1.equals( COMP_TYPE.MYPLAYER );

        boolean contact2Ground = type2.equals( COMP_TYPE.GROUND );
        if ( contact1Player && contact2Ground ) {
            RemoveByLevelComponent zoneComponent = MyMapper.REMOVE_BY_ZONE.get( entity02 );

            int lastID = DynamicLevels.getCurrent();
            int newID = zoneComponent.id;
            DynamicLevels.setCurrent( newID );
            if ( !MyMapper.GROUNDED.get( entity01 ).grounded && lastID != newID ) {
                DynamicLevels.reload();
            }
            MyMapper.GROUNDED.get( entity01 ).grounded = true;
        }

        boolean contact2Ladder = type2.equals( COMP_TYPE.LADDER );
        if ( contact1Player && contact2Ladder ) {
            MyPlayer.onLadder = true;
        }

        boolean contact2Collectable = type2.equals( COMP_TYPE.COLLECTABLE );
        boolean outOfCamera = MyMapper.OUT_OF_CAMERA.has( entity02 );
        if ( !outOfCamera ) {
            if ( contact1Player && contact2Collectable ) {
                MyPlayer.addToBag( MyMapper.COLLECTABLE.get( entity02 ).item );
                entity02.add( new OutOfCameraComponent() );
                entity02.add( new RemoveByTimeComponent( 0 ) );

                // Устанавливаем в levelObject флаг, чтобы предмет
                // не создался снова, при перезагрузке уровня
                if ( MyMapper.LEVEL_OBJECT.has( entity02 ) ) {
                    MyMapper.LEVEL_OBJECT.get( entity02 ).levelObject.useEntity();
                }

                // show bubbles
                if ( MyMapper.PHYSICAL.has( entity02 ) ) {
                    Vector3 bubblePosition = new Vector3();
                    MyMapper.PHYSICAL.get( entity02 ).body.getWorldTransform()
                                                          .getTranslation( bubblePosition );
                    for ( int i = 0; i < 5; i++ ) {
                        float bubbleTime = MathUtils.random( 0.05f, 0.25f );
                        /*AshleyWorld.getPooledEngine().addEntity(
                                EntitySubs.LightBubblesEffect( bubblePosition, bubbleTime * 6 ) );*/

                        Entity entity = AshleyWorld.getPooledEngine().createEntity();
                        entity.add( DecalSubs.BubbleEffect( bubbleTime ) );
                        entity.add( new PositionComponent( bubblePosition ) );
                        entity.add( new RemoveByTimeComponent( bubbleTime ) );
                        AshleyWorld.getPooledEngine().addEntity( entity );
                    }
                }
            }
        }
    }


    private void endContactEntities ( Entity entity01, Entity entity02 ) {
        COMP_TYPE type1 = MyMapper.TYPE.get( entity01 ).type;
        COMP_TYPE type2 = MyMapper.TYPE.get( entity02 ).type;

        if ( DEBUG.CONTACTS.get() ) {
            stringBuilder.setLength( 0 );
            stringBuilder.append( type1 );
            stringBuilder.append( " -> " );
            stringBuilder.append( type2 );
            Gdx.app.debug( "endContact", stringBuilder.toString() );
        }

        boolean contact1Player = type1.equals( COMP_TYPE.MYPLAYER );
        boolean contact2Ground = type2.equals( COMP_TYPE.GROUND );
        if ( contact1Player && contact2Ground ) {
            MyMapper.GROUNDED.get( entity01 ).grounded = false;
            DynamicLevels.reload();
            //return;
        }

        boolean contact2Ladder = type2.equals( COMP_TYPE.LADDER );
        if ( contact1Player && contact2Ladder ) {
            MyPlayer.onLadder = false;
        }
    }


    public void setEntities ( ImmutableArray< Entity > entities ) {
        this.entities = entities;
    }


    @Override
    public void dispose () {
        super.dispose();

        entities = null;
        stringBuilder = null;
    }
}