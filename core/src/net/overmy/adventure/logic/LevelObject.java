package net.overmy.adventure.logic;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import net.overmy.adventure.AshleySubs;
import net.overmy.adventure.DEBUG;
import net.overmy.adventure.ashley.components.NPCAction;
import net.overmy.adventure.ashley.components.RemoveByTimeComponent;
import net.overmy.adventure.resources.ModelAsset;

/**
 * Created by Andrey (cb) Mikheev
 * 17.03.2017
 */

public class LevelObject {

    ModelAsset modelAsset;
    protected Vector3     position;
    private   OBJECT_TYPE type;
    private   Item               item        = null;
    private   TextBlock          textBlock   = null;
    private   Array< NPCAction > actionArray = null;
    protected Entity             entity      = null;
    private   boolean            used        = false;
    private   float              height      = 0.0f;


    LevelObject ( OBJECT_TYPE type, ModelAsset models, Vector3 position ) {
        this.type = type;
        this.modelAsset = models;
        this.position = position;
    }


    LevelObject ( OBJECT_TYPE type, ModelAsset models, Vector3 position, float height ) {
        this.type = type;
        this.modelAsset = models;
        this.position = position;
        this.height = height;
    }


    LevelObject ( OBJECT_TYPE type, Item id, ModelAsset models, Vector3 position ) {
        this.type = type;
        this.item = id;
        this.modelAsset = models;
        this.position = position;
    }


    LevelObject ( OBJECT_TYPE type, TextBlock textBlock, Array< NPCAction > actionArray,
                  ModelAsset models, Vector3 position ) {
        this.type = type;
        this.textBlock = textBlock;
        this.modelAsset = models;
        this.position = position;
        this.actionArray = actionArray;
    }


    LevelObject ( OBJECT_TYPE type, Array< NPCAction > actionArray,
                  ModelAsset models, Vector3 position ) {
        this.type = type;
        this.modelAsset = models;
        this.position = position;
        this.actionArray = actionArray;
    }


    public void useEntity () {
        used = true;
        entity.add( new RemoveByTimeComponent( 0 ) );
        entity = null;

        Gdx.app.debug( "useEntity", "" + item );
    }


    void removeEntity () {
        if ( entity != null ) {
            entity.add( new RemoveByTimeComponent( 0 ) );
        }
        entity = null;
    }


    void buildEntity () {
        if ( entity != null || used ) {
            return;
        }

        if ( DEBUG.DYNAMIC_LEVELS.get() ) {
            Gdx.app.debug( "Need to build OBJECT", "" + this.type );
        }

        // Из свича вынесены вверх одинаковые кусочки для всех вариантов сборки
        //entity = AshleyWorld.getPooledEngine().createEntity();

        switch ( type ) {
            case LADDER:
                entity = AshleySubs.createLadder( position, height );
                break;

            case PICKABLE:
                entity = AshleySubs.createPickable( position, modelAsset, item, this );
                break;

            case BOX:
                entity = AshleySubs.createCrate( position, modelAsset, item, this );
                break;

            case COLLECTABLE:
                entity = AshleySubs.createCollectable( position, modelAsset, item, this );
                break;

            case HOVER_COLLECTABLE:
                entity = AshleySubs.createHoverCollectable( position, modelAsset, item, this );
                break;

            case NPC:
                entity = AshleySubs.createNPC( position, modelAsset, textBlock, actionArray );
                break;

            case ENEMY:
                entity = AshleySubs.createEnemy( position, modelAsset, actionArray );
                break;

            case WEAPON:
                entity = AshleySubs.createWeapon( position, modelAsset, item, this );
                break;
        }

        //AshleyWorld.getPooledEngine().addEntity( entity );
    }


    void buildModel () {
        if ( entity == null && !used ) {
            modelAsset.build();
        }
    }
}
