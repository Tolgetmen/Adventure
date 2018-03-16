package net.overmy.adventure;

/*
      Created by Andrey Mikheev on 10.10.2017
      Contact me → http://vk.com/id17317
 */

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import net.overmy.adventure.ashley.DecalSubs;
import net.overmy.adventure.ashley.MyMapper;
import net.overmy.adventure.ashley.components.ActorComponent;
import net.overmy.adventure.ashley.components.AnimationComponent;
import net.overmy.adventure.ashley.components.COMP_TYPE;
import net.overmy.adventure.ashley.components.GroundedComponent;
import net.overmy.adventure.ashley.components.ModelComponent;
import net.overmy.adventure.ashley.components.MyPlayerComponent;
import net.overmy.adventure.ashley.components.MyWeaponComponent;
import net.overmy.adventure.ashley.components.PhysicalComponent;
import net.overmy.adventure.ashley.components.PositionComponent;
import net.overmy.adventure.ashley.components.RemoveByTimeComponent;
import net.overmy.adventure.ashley.components.TypeOfComponent;
import net.overmy.adventure.logic.Item;
import net.overmy.adventure.logic.ItemInBagg;
import net.overmy.adventure.resources.FontAsset;
import net.overmy.adventure.resources.GameColor;
import net.overmy.adventure.resources.ModelAsset;
import net.overmy.adventure.resources.Settings;
import net.overmy.adventure.resources.SoundAsset;
import net.overmy.adventure.resources.TextureAsset;
import net.overmy.adventure.utils.UIHelper;

import java.util.ArrayList;

public final class MyPlayer {
    private static final Vector2    v2Position     = new Vector2();
    private static final Vector2    direction      = new Vector2();
    private static final Vector3    velocity       = new Vector3();
    private static       Matrix4    bodyTransform  = new Matrix4();
    private static final Vector3    notFilteredPos = new Vector3();
    private static final Vector3    filteredPos    = new Vector3();
    private static       ModelAsset modelAsset     = null;
    private static       Entity     playerEntity   = null;
    private static       boolean    jump           = false;
    private static       boolean    attack         = false;
    private static       float      modelAngle     = 0.0f;
    private static       float      dustTime       = 0.1f;

    private static boolean weaponInHand = false;

    private static Node rightArmNode = null;

    private static ArrayList< ItemInBagg > bag = null;


    public static ArrayList< ItemInBagg > getBag () {
        return bag;
    }


    private static float moveY = 0.0f;

    private static float speed;

    public static  boolean    onLadder = false;
    private static SoundAsset walk     = null;

    private static btRigidBody playerBody = null;


    private static ModelInstance modelInstanceWeaponInHand = null;
    private static Entity        entityWeaponInHand        = null;
    private static ItemInBagg    itemInHand                = null;

    public static boolean isAttacking = false;


    private MyPlayer () {
    }


    public static void load () {
        int n = ModelAsset.PLAYER01.ordinal() + Settings.PLAYER_MODEL.getInteger();
        modelAsset = ModelAsset.values()[ n ];

        modelAsset.load();

        //Gdx.app.debug( "" + modelAsset.toString(), "n of model " + n );
    }


    public static void addToBag ( Item item ) {
        //Gdx.app.debug( "Добавлено в сумку", "" + item.getName() );

        boolean alreadyPresent = false;

        if ( bag.size() > 0 ) {
            for ( ItemInBagg itemInBag : bag ) {
                if ( itemInBag.item.equals( item ) ) {
                    itemInBag.count++;
                    alreadyPresent = true;
                }
            }
        }

        if ( !alreadyPresent ) {
            bag.add( new ItemInBagg( item ) );
        }
    }


    public static void init () {
        bag = new ArrayList< ItemInBagg >();

        if ( playerEntity != null ) {
            return;
        }

        modelAsset.build();

        final ModelInstance modelInstance = modelAsset.get();

        // Здесь достаём нод руки игрока
        // FIXME
        //rightArmNode = modelInstance.getNode( "arm_r03", true );
        rightArmNode = modelInstance.getNode( "rightArm", true );

        modelInstance.transform.setToTranslation( new Vector3( 0, 3, 0 ) );

        modelInstance.materials.get( 0 ).clear();
        //modelInstance.materials.get( 0 ).set( TextureAttribute.createDiffuse( region ) );
        modelInstance.materials.get( 0 )
                               .set( ColorAttribute.createDiffuse( GameColor.SQUIREL.get() ) );

        final PhysicalBuilder physicalBuilder = new PhysicalBuilder()
                .setModelInstance( modelInstance )
                .defaultMotionState()
                .setMass( 20.0f )
                //.capsuleShape( 0.5f, 0.2f )
                .capsuleShape()
                .setCollisionFlag( btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT )
                .setCallbackFlag( BulletWorld.PLAYER_FLAG )
                .setCallbackFilter( BulletWorld.ALL_FLAG )
                .disableDeactivation();

        PhysicalComponent physicalComponent = physicalBuilder.buildPhysicalComponent();
        physicalComponent.body.setFriction( 0.1f );

        playerBody = physicalComponent.body;
        //body.setSpinningFriction( 0.1f );
        //body.setRollingFriction( 0.1f );

        playerEntity = AshleyWorld.getPooledEngine().createEntity();
        playerEntity.add( physicalComponent );
        playerEntity.add( new ModelComponent( modelInstance ) );
        playerEntity.add( new AnimationComponent( modelInstance ) );
        playerEntity.add( new GroundedComponent() );
        playerEntity.add( new TypeOfComponent( COMP_TYPE.MYPLAYER ) );
        playerEntity.add( new MyPlayerComponent() );

        AshleyWorld.getPooledEngine().addEntity( playerEntity );

        onLadder = false;

        walk = SoundAsset.Step3;
        walk.playLoop();
        walk.setVolume( 0.0f );
    }

/*

    public Matrix4 getRightArm() {
        // set transform node of SWORD
        armMatrix.set( rightArmNode.calculateWorldTransform() );
        return armMatrix;
    }
*/


    public static void updateControls ( float deltaTime ) {

        if ( Gdx.input.isKeyJustPressed( Input.Keys.SPACE ) ) {
            startJump();
        }

        updateAnimation( deltaTime );

        final boolean playerOnGround = MyMapper.GROUNDED.get( playerEntity ).grounded;

        // Двигаем или останавливаем физическое тело
        velocity.set( direction.x, 0, direction.y );
        velocity.scl( speed );
        if ( !onLadder ) {
            velocity.add( 0, playerBody.getLinearVelocity().y, 0 );
        } else {
            velocity.add( 0, -moveY * 5, 0 );
        }

        playerBody.setLinearVelocity( velocity );
        //body.applyCentralImpulse( velocity );

        //body.setFriction( 0.1f );
        //body.setSpinningFriction( 0.1f );
        //body.setRollingFriction( 0.1f );

        if ( jump && !attack ) {
            if ( playerOnGround ) {
                final float jumpSpeed = 148.0f;
                velocity.set( direction.x, jumpSpeed, direction.y );
                //body.setLinearVelocity( velocity );
                playerBody.applyCentralImpulse( velocity );

                SoundAsset.Jump1.play();

                final int JUMP = 3;
                final AnimationComponent animationComponent = MyMapper.ANIMATION.get(
                        playerEntity );
                animationComponent.play( JUMP, 2.0f );

                isAttacking = false;
            }
            jump = false;
        }

        if ( attack ) {
            final int HIT = 2;
            final AnimationComponent animationComponent = MyMapper.ANIMATION.get(
                    playerEntity );
            animationComponent.play( HIT, 2.4f );
            attack = false;

            isAttacking = true;
        }

        playerBody.getWorldTransform( bodyTransform );
        bodyTransform.getTranslation( notFilteredPos );
        bodyTransform.idt();
        bodyTransform.setToTranslation( notFilteredPos );
        bodyTransform.rotate( Vector3.Y, modelAngle );
        playerBody.proceedToTransform( bodyTransform );

/*        // Применяем физику к рендер-модели
        final float ALPHA = 0.45f;
        filteredPos.x = filteredPos.x + ALPHA * ( notFilteredPos.x - filteredPos.x );
        filteredPos.y = filteredPos.y + ALPHA * ( notFilteredPos.y - filteredPos.y );
        filteredPos.z = filteredPos.z + ALPHA * ( notFilteredPos.z - filteredPos.z );
        bodyTransform.setToTranslation( filteredPos );
        bodyTransform.rotate( Vector3.Y, modelAngle );*/
/*

        // Поворачиваем модель контроллером на экране
        // после работы PhysicalSystem
        body.getWorldTransform( bodyTransform );
        bodyTransform.getTranslation( notFilteredPos );
        bodyTransform.idt();
        bodyTransform.setToTranslation( notFilteredPos );
        bodyTransform.rotate( Vector3.Y, modelAngle );
        body.proceedToTransform( bodyTransform );

        // Применяем физику к рендер-модели
        final float ALPHA = 0.45f;
        filteredPos.x = filteredPos.x + ALPHA * ( notFilteredPos.x - filteredPos.x );
        filteredPos.y = filteredPos.y + ALPHA * ( notFilteredPos.y - filteredPos.y );
        filteredPos.z = filteredPos.z + ALPHA * ( notFilteredPos.z - filteredPos.z );
        bodyTransform.setToTranslation( filteredPos );
        bodyTransform.rotate( Vector3.Y, modelAngle );
*/

        //MyMapper.MODEL.get( playerEntity ).modelInstance.transform.set( bodyTransform );

        MyCamera.setCameraPosition( notFilteredPos );

        v2Position.set( notFilteredPos.x, notFilteredPos.z );
    }


    private static void updateAnimation ( float deltaTime ) {

        final AnimationComponent animationComponent = MyMapper.ANIMATION.get( playerEntity );
        final String ID_CURRENT = animationComponent.getID();
        final String ID_ATTACK = "ATTACK";
        final String ID_RUN = "RUN";
        final String ID_IDLE = "IDLE";

        final boolean playerOnGround = MyMapper.GROUNDED.get( playerEntity ).grounded;
        final boolean playerInIDLE = ID_IDLE.equals( ID_CURRENT );
        final boolean playerIsRunning = ID_RUN.equals( ID_CURRENT );
        final boolean playerHitSomething = ID_ATTACK.equals( ID_CURRENT );
//        final boolean playerJump = ID_JUMP.equals( ID_CURRENT );

        final int IDLE = 0;
        final int RUN = 1;
        final int ATTACK = 2;

        // SET sound of walking steps
        if ( !onLadder ) {
            if ( direction.len() > 0 && playerOnGround ) {
                float walkSpeed = ( 2.5f + direction.len() * 0.5f ) / 4;
                walk.setVolume( 1.0f );
                walk.setPitch( walkSpeed );
            } else {
                walk.setVolume( 0.0f );
            }
        } else {
            walk.setVolume( 0.0f );
        }

        speedUpTime -= deltaTime;
        float extraSpeed = 0.0f;
        if ( speedUpTime > 0 ) {
            int textSpeed = (int) speedUpTime + 1;
            speedUpTimerLabel.setText( "" + textSpeed );
            GlyphLayout layout = speedUpTimerLabel.getGlyphLayout();
            int iconSize = (int) ( Core.HEIGHT * 0.1f );
            speedUpTimerLabel.setPosition(
                    Core.WIDTH - iconSize * 2.5f - layout.width / 2,
                    Core.HEIGHT - iconSize * 1.5f - layout.height );
            extraSpeed = 5.0f;
        }

        final float directionLen = direction.len();
        // Мы управляем персонажем джойстиком
        if ( directionLen != 0 ) {
            // Персонаж на земле
            if ( playerOnGround ) {
                final float animationSpeed = 3.0f + 2.0f * directionLen;
                if ( playerIsRunning && !playerHitSomething ) {
                    animationComponent.queue( RUN, animationSpeed );
                } else {
                    animationComponent.play( RUN, animationSpeed );
                    isAttacking = false;
                }
            }
            // Персонаж в воздухе
            else {
                if ( playerInIDLE && !playerHitSomething ) {
                    animationComponent.queue( IDLE, 2.0f );
                } else {
                    animationComponent.play( IDLE, 2.0f );
                    isAttacking = false;
                }
            }
            final float runSpeed = 5.0f;
            speed = ( runSpeed + extraSpeed ) * directionLen;

            direction.nor();
            direction.rotate( -MyCamera.getCameraAngle() );

            // Сохраняем угол для поворота модели
            modelAngle = 90 - direction.angle();

            // СОздаем пыль под ногами

            dustTime -= deltaTime;
            if ( dustTime < 0 ) {
                dustTime = MathUtils.random( 0.05f, 0.25f );
                //notFilteredPos.sub( 0, 0.5f, 0 );

                Entity entity = AshleyWorld.getPooledEngine().createEntity();
                entity.add( DecalSubs.LightDustEffect( dustTime * 6 ) );
                entity.add( new PositionComponent( notFilteredPos ) );
                entity.add( new RemoveByTimeComponent( dustTime * 6 ) );
                AshleyWorld.getPooledEngine().addEntity( entity );
            }
        }
        // Мы не управляем персонажем джойстиком
        else {
            // Персонаж на земле
            if ( playerOnGround ) {
                if ( !playerHitSomething ) {
                    if ( playerInIDLE ) {
                        animationComponent.queue( IDLE, 2.0f );
                    } else {
                        animationComponent.play( IDLE, 2.0f );
                    }
                }
            }
            speed = 0.0f;
        }
    }


    public static void startJump () {
        jump = true;
    }


    public static void startAttack () {
        attack = true;
    }

    //static Vector2 tmpDirection = new Vector2(  );


    public static void move ( float x, float y ) {
        direction.set( x, y );
        moveY = y;
    }


    public static void dispose () {
        if ( bag != null ) {
            bag.clear();
        }
        bag = null;

        modelAsset = null;
        playerEntity = null;

        if ( walk != null ) {
            walk.stop();
        }
        walk = null;

        playerBody = null;
        rightArmNode = null;
    }


    public static Vector2 getPosition () {
        return v2Position;
    }


    public static btRigidBody getBody () {
        return playerBody;
    }


    private static float speedUpTime = 0.0f;

    private static Label speedUpTimerLabel = null;


    public static void useItemInBag ( ItemInBagg item ) {

        boolean broomIsItem = item.item.equals( Item.BROOM_WEAPON );
        boolean kalashIsItem = item.item.equals( Item.KALASH_WEAPON );
        boolean rakeIsItem = item.item.equals( Item.RAKE_WEAPON );
        boolean borderIsItem = item.item.equals( Item.BORDER_WEAPON );

        if ( broomIsItem || kalashIsItem || rakeIsItem || borderIsItem ) {
            if ( weaponInHand ) {
                modelInstanceWeaponInHand.nodes.get( 0 ).detach();
                entityWeaponInHand.add( new RemoveByTimeComponent( 0 ) );
                entityWeaponInHand = null;
                bag.add( itemInHand );
            }

            itemInHand = item;
            bag.remove( item );
        } else {
            if ( item.count < 2 ) {
                bag.remove( item );
            } else {
                item.count -= 1;
            }
        }

        switch ( item.item ) {
            case RED_BOTTLE:
                speedUpTime = 10.0f;

                if ( speedUpTimerLabel != null ) {
                    speedUpTimerLabel = null;
                }

                int iconSize = (int) ( Core.HEIGHT * 0.1f );
                Image iconImage2 = new Image( TextureAsset.CD.getSprite() );
                iconImage2.setSize( iconSize, iconSize );
                iconImage2.setOrigin( iconSize / 2, iconSize / 2 );
                iconImage2.setPosition( Core.WIDTH - iconSize * 3.0f, Core.HEIGHT - iconSize );
                iconImage2.addAction( Actions.forever(
                        Actions.sequence(
                                Actions.rotateTo( 0, 0 ),
                                Actions.rotateTo( 360.0f, 2.0f )
                                        )
                                                     ) );

                Image iconImage = item.item.getImage( iconSize, iconSize );
                iconImage.setPosition( Core.WIDTH - iconSize * 3.0f, Core.HEIGHT - iconSize );

                ActorComponent actorComponent = new ActorComponent();
                actorComponent.group.addActor( iconImage2 );
                actorComponent.group.addActor( iconImage );

                int textSpeed = (int) speedUpTime + 1;
                speedUpTimerLabel = UIHelper.Label( "" + textSpeed, FontAsset.ACTION_TEXT );
                GlyphLayout layout = speedUpTimerLabel.getGlyphLayout();
                speedUpTimerLabel.setPosition(
                        Core.WIDTH - iconSize * 2.5f - layout.width / 2,
                        Core.HEIGHT - iconSize * 1.5f - layout.height );
                actorComponent.group.addActor( speedUpTimerLabel );

                Entity timerEntity = AshleyWorld.getPooledEngine().createEntity();
                timerEntity.add( actorComponent );
                timerEntity.add( new RemoveByTimeComponent( speedUpTime ) );
                AshleyWorld.getPooledEngine().addEntity( timerEntity );

                break;

            case KALASH_WEAPON:
                // create weapon in ashley
                modelInstanceWeaponInHand = ModelAsset.KALASH_WEAPON1.get();
                // attach only model without physics
                modelInstanceWeaponInHand.nodes.get( 0 ).attachTo( rightArmNode );

                PhysicalBuilder physicalBuilderWEAPON = new PhysicalBuilder()
                        .setModelInstance( modelInstanceWeaponInHand );

                physicalBuilderWEAPON
                        .defaultMotionState()
                        .zeroMass()
                        .hullShape()
                        .setCollisionFlag( btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE )
                        .setCallbackFlag( BulletWorld.MYWEAPON_FLAG )
                        .setCallbackFilter( BulletWorld.ALL_FLAG )
                        .disableDeactivation();

                entityWeaponInHand = AshleyWorld.getPooledEngine().createEntity();
                entityWeaponInHand.add( new TypeOfComponent( COMP_TYPE.WEAPON ) );
                entityWeaponInHand.add( new MyWeaponComponent( rightArmNode, bodyTransform ) );
                entityWeaponInHand.add( physicalBuilderWEAPON.buildPhysicalComponent() );
                AshleyWorld.getPooledEngine().addEntity( entityWeaponInHand );

                weaponInHand = true;

                break;

            case BORDER_WEAPON:
                // create weapon in ashley
                modelInstanceWeaponInHand = ModelAsset.BORDER_WEAPON1.get();
                // attach only model without physics
                modelInstanceWeaponInHand.nodes.get( 0 ).attachTo( rightArmNode );

                PhysicalBuilder physicalBuilderBORDER_WEAPON = new PhysicalBuilder()
                        .setModelInstance( modelInstanceWeaponInHand );

                physicalBuilderBORDER_WEAPON
                        .defaultMotionState()
                        .zeroMass()
                        .hullShape()
                        .setCollisionFlag( btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE )
                        .setCallbackFlag( BulletWorld.MYWEAPON_FLAG )
                        .setCallbackFilter( BulletWorld.ALL_FLAG )
                        .disableDeactivation();

                entityWeaponInHand = AshleyWorld.getPooledEngine().createEntity();
                entityWeaponInHand.add( new TypeOfComponent( COMP_TYPE.WEAPON ) );
                entityWeaponInHand.add( new MyWeaponComponent( rightArmNode, bodyTransform ) );
                entityWeaponInHand.add( physicalBuilderBORDER_WEAPON.buildPhysicalComponent() );
                AshleyWorld.getPooledEngine().addEntity( entityWeaponInHand );

                weaponInHand = true;

                break;

            case BROOM_WEAPON:
                // create weapon in ashley
                modelInstanceWeaponInHand = ModelAsset.BROOM_WEAPON1.get();
                // attach only model without physics
                modelInstanceWeaponInHand.nodes.get( 0 ).attachTo( rightArmNode );

                PhysicalBuilder physicalBuilderBROOM_WEAPON = new PhysicalBuilder()
                        .setModelInstance( modelInstanceWeaponInHand );

                physicalBuilderBROOM_WEAPON
                        .defaultMotionState()
                        .zeroMass()
                        .hullShape()
                        .setCollisionFlag( btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE )
                        .setCallbackFlag( BulletWorld.MYWEAPON_FLAG )
                        .setCallbackFilter( BulletWorld.ALL_FLAG )
                        .disableDeactivation();

                entityWeaponInHand = AshleyWorld.getPooledEngine().createEntity();
                entityWeaponInHand.add( new TypeOfComponent( COMP_TYPE.WEAPON ) );
                entityWeaponInHand.add( new MyWeaponComponent( rightArmNode, bodyTransform ) );
                entityWeaponInHand.add( physicalBuilderBROOM_WEAPON.buildPhysicalComponent() );
                AshleyWorld.getPooledEngine().addEntity( entityWeaponInHand );

                weaponInHand = true;

                break;

            case RAKE_WEAPON:
                // create weapon in ashley
                modelInstanceWeaponInHand = ModelAsset.RAKE_WEAPON1.get();
                // attach only model without physics
                modelInstanceWeaponInHand.nodes.get( 0 ).attachTo( rightArmNode );

                PhysicalBuilder physicalBuilderSwordWEAPON = new PhysicalBuilder()
                        .setModelInstance( modelInstanceWeaponInHand );

                physicalBuilderSwordWEAPON
                        .defaultMotionState()
                        .zeroMass()
                        .hullShape()
                        .setCollisionFlag( btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE )
                        .setCallbackFlag( BulletWorld.MYWEAPON_FLAG )
                        .setCallbackFilter( BulletWorld.ALL_FLAG )
                        .disableDeactivation();

                entityWeaponInHand = AshleyWorld.getPooledEngine().createEntity();
                entityWeaponInHand.add( new TypeOfComponent( COMP_TYPE.WEAPON ) );
                entityWeaponInHand.add( new MyWeaponComponent( rightArmNode, bodyTransform ) );
                entityWeaponInHand.add( physicalBuilderSwordWEAPON.buildPhysicalComponent() );
                AshleyWorld.getPooledEngine().addEntity( entityWeaponInHand );

                weaponInHand = true;

                break;
        }
    }
}
