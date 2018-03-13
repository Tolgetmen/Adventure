package net.overmy.adventure.screen;

/*
      Created by Andrey Mikheev on 10.10.2017
      Contact me → http://vk.com/id17317
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.TimeUtils;

import net.overmy.adventure.AshleyWorld;
import net.overmy.adventure.BulletWorld;
import net.overmy.adventure.Core;
import net.overmy.adventure.DEBUG;
import net.overmy.adventure.MyCamera;
import net.overmy.adventure.MyGdxGame;
import net.overmy.adventure.MyPlayer;
import net.overmy.adventure.MyRender;
import net.overmy.adventure.ashley.MyMapper;
import net.overmy.adventure.ashley.components.TYPE_OF_INTERACT;
import net.overmy.adventure.ashley.systems.DecalSystem;
import net.overmy.adventure.ashley.systems.InteractSystem;
import net.overmy.adventure.ashley.systems.NPCSystem;
import net.overmy.adventure.ashley.systems.RenderSystem;
import net.overmy.adventure.logic.DynamicLevels;
import net.overmy.adventure.logic.Item;
import net.overmy.adventure.logic.ItemInBagg;
import net.overmy.adventure.logic.TextBlock;
import net.overmy.adventure.resources.FontAsset;
import net.overmy.adventure.resources.GameColor;
import net.overmy.adventure.resources.IMG;
import net.overmy.adventure.resources.MusicAsset;
import net.overmy.adventure.resources.SoundAsset;
import net.overmy.adventure.resources.TextDialogAsset;
import net.overmy.adventure.resources.TextureAsset;
import net.overmy.adventure.utils.GFXHelper;
import net.overmy.adventure.utils.LoadIndicator;
import net.overmy.adventure.utils.UIHelper;

import java.util.ArrayList;

public class GameScreen extends Base2DScreen {
    private Image         jumpButton     = null;
    private Touchpad      touchpad       = null;
    private LoadIndicator indicatorGroup = null;
    private Group         interactGroup  = null;
    private Group         touchPadGroup  = null;

    private InteractSystem interactSystem = null;

    private GUI_TYPE guiType;

    private Group gameGroup = null;

    private long startTime;
    private StringBuilder log = new StringBuilder();


    private boolean readyToPick = false;


    public GameScreen ( MyGdxGame game ) {
        super( game );
    }


    private SpriteBatch spriteBatch;
    private Texture     bg;


    private static ArrayList< Vector3 > pushedPositions = new ArrayList< Vector3 >();


    @Override
    public void show () {
        super.show();

        AshleyWorld.getPooledEngine().getSystem( NPCSystem.class ).setWalkSound();

        interactSystem = AshleyWorld.getPooledEngine().getSystem( InteractSystem.class );

        bg = TextureAsset.BG_GRADIENT.get();
        spriteBatch = MyRender.getSpriteBatch();

        MyPlayer.init();

        touchPadGroup = new Group();
        MyRender.getStage().addActor( indicatorGroup = initLoadIndicator() );
        MyRender.getStage().addActor( gameGroup = new Group() );

        showGameGUI();

        // Music environment
        MusicAsset.WINDFILTER.play( true );

        if ( DEBUG.GAME_MASTER_MODE.get() ) {
           /* float inGameIconSize = Core.HEIGHT * 0.16f;

            final Image showIngameMenuImage2 = IMG.INGAME.getImageActor( inGameIconSize, inGameIconSize );
            showIngameMenuImage2.setPosition( Core.WIDTH - inGameIconSize*2, Core.HEIGHT - inGameIconSize );
            MyRender.getStage().addActor( showIngameMenuImage2 );

            showIngameMenuImage2.addListener( new ClickListener() {
                public void clicked ( InputEvent event, float x, float y ) {
                    SoundAsset.Step1.get().loop( 1, 1, 1 );

                    UIHelper.clickAnimation( showIngameMenuImage2 );
                }
            } );

            final Image showIngameMenuImage3 = IMG.INGAME.getImageActor( inGameIconSize, inGameIconSize );
            showIngameMenuImage3.setPosition( Core.WIDTH - inGameIconSize*3, Core.HEIGHT - inGameIconSize );
            MyRender.getStage().addActor( showIngameMenuImage3 );

            showIngameMenuImage3.addListener( new ClickListener() {
                public void clicked ( InputEvent event, float x, float y ) {
                    SoundAsset.Step2.get().loop( 1, 1, 1 );

                    UIHelper.clickAnimation( showIngameMenuImage3 );
                }
            } );

            final Image showIngameMenuImage4 = IMG.INGAME.getImageActor( inGameIconSize, inGameIconSize );
            showIngameMenuImage4.setPosition( Core.WIDTH - inGameIconSize*4, Core.HEIGHT - inGameIconSize );
            MyRender.getStage().addActor( showIngameMenuImage4 );

            showIngameMenuImage4.addListener( new ClickListener() {
                public void clicked ( InputEvent event, float x, float y ) {
                    SoundAsset.Step3.get().loop( 1, 1, 1 );

                    UIHelper.clickAnimation( showIngameMenuImage4 );
                }
            } );
*/

            String helpString = "ENTER - push position\n1- show bonus pos\n2-show NPC move pos";
            Label ingameMenuTitle = UIHelper.Label( helpString, FontAsset.LOCATION_TEXT );
            float fontOffset = ingameMenuTitle.getHeight() * 1.5f;
            ingameMenuTitle.setPosition( fontOffset,
                                         Core.HEIGHT - fontOffset );
            MyRender.getStage().addActor( ingameMenuTitle );
        }
    }


    @Override
    public boolean touchDragged ( float x, float y ) {
        if ( guiType == GUI_TYPE.INGAME_MENU ) {
            return false;
        }

        final float SPEED_BY_X = 0.02f;
        final float SPEED_BY_Y = 0.0005f;

        MyCamera.addCameraAngle( x * SPEED_BY_X );
        MyCamera.addVerticalDirection( -y * SPEED_BY_Y );
        return true;
    }


    @Override
    public void draw ( float delta ) {
        spriteBatch.setColor( 1, 1, 1, 1 );
        spriteBatch.begin();
        spriteBatch.draw( bg, 0, 0, Core.WIDTH, Core.HEIGHT );
        spriteBatch.end();

        AshleyWorld.update( delta );

        if ( DEBUG.PHYSICAL_MESH.get() ) {
            BulletWorld.drawDebug();
        }

        MyRender.getDecalBatch().flush();
    }


    @Override
    public void update ( float delta ) {
        super.update( delta );

        if ( DEBUG.GAME_MASTER_MODE.get() ) {
            if ( Gdx.input.isKeyJustPressed( Input.Keys.ENTER ) ) {
                Matrix4 thisTransform = MyPlayer.getBody().getWorldTransform();
                Vector3 thisPosition = new Vector3();
                thisTransform.getTranslation( thisPosition );
                pushedPositions.add( thisPosition );

                Gdx.app.debug( "Position " + thisPosition, "pushed" );
            }

            if ( Gdx.input.isKeyJustPressed( Input.Keys.BACKSPACE ) ) {
                pushedPositions.clear();
                Gdx.app.debug( "Positions ", "cleared" );
            }

            // GameMaster Mode
            // add star
            if ( Gdx.input.isKeyJustPressed( Input.Keys.NUM_1 ) ) {
                StringBuilder stringBuilder = new StringBuilder();

                for ( Vector3 pushed : pushedPositions ) {
                    stringBuilder.append( "objects.add( hoverCoin( new Vector3(" );
                    stringBuilder.append( pushed.x );
                    stringBuilder.append( "f, " );
                    stringBuilder.append( pushed.y );
                    stringBuilder.append( "f, " );
                    stringBuilder.append( pushed.z );
                    stringBuilder.append( "f) ) );\n" );
                }

                Gdx.app.debug( "Pushed positions", "\n" + stringBuilder.toString() );
            }
            // add move point
            if ( Gdx.input.isKeyJustPressed( Input.Keys.NUM_2 ) ) {
                StringBuilder stringBuilder = new StringBuilder();

                for ( Vector3 pushed : pushedPositions ) {
                    //queue.add( new NPCAction( NPC_ACTION_ID.MOVE, new Vector2( 15.5f, -3.166f ), 10.0f ) );
                    stringBuilder.append(
                            "queue.add( new NPCAction( NPC_ACTION_ID.MOVE, new Vector2(" );
                    stringBuilder.append( pushed.x );
                    stringBuilder.append( "f, " );
                    //stringBuilder.append( pushed.y );
                    //stringBuilder.append( "f, " );
                    stringBuilder.append( pushed.z );
                    stringBuilder.append( "f), 10.0f ) );\n" );
                }

                Gdx.app.debug( "Pushed positions", "\n" + stringBuilder.toString() );
            }
        }

        DynamicLevels.update( delta );
        MyPlayer.updateControls( delta );
        MyCamera.update( delta );

        if ( guiType == GUI_TYPE.GAME_GUI ) {
            MyPlayer.move( touchpad.getKnobPercentX(), -touchpad.getKnobPercentY() );
        }

        if ( DEBUG.SHOW_FPS.get() ) {
            if ( TimeUtils.nanoTime() - startTime > 1000000000 ) /* 1,000,000,000ns == one second */ {
                log.setLength( 0 );
                log.append( Gdx.graphics.getFramesPerSecond() );

                RenderSystem rend = AshleyWorld.getPooledEngine().getSystem( RenderSystem.class );
                int models = rend.getModelsCount();
                int totalModels = rend.getTotalModelsCount();
                log.append( " Models=" );
                log.append( models );
                log.append( "/" );
                log.append( totalModels );

                int d = AshleyWorld.getPooledEngine()
                                   .getSystem( DecalSystem.class )
                                   .getDecalCount();
                log.append( " Decals=" );
                log.append( d );

                log.append( " ░▒▓" );

                Gdx.app.log( "▓▒░ FPS", log.toString() );
                startTime = TimeUtils.nanoTime();
            }
        }

        // Работа интерактивной системы

        TYPE_OF_INTERACT typeOfInteract = interactSystem.getCurrentType();

        if ( !typeOfInteract.equals( TYPE_OF_INTERACT.EMPTY ) ) {
            if ( !readyToPick ) {
                readyToPick = true;
                log.setLength( 0 );
                log.append( typeOfInteract );
                log.append( " " );
                if ( interactSystem.getCurrentItem() != null ) {
                    log.append( interactSystem.getCurrentItem() );
                } else {
                    log.append( interactSystem.getCurrentTextBlock() );
                }
                Label pickText = UIHelper.Label( log.toString(),
                                                 FontAsset.LOCATION_TITLE );
                interactGroup.addActor( pickText );
                interactGroup.addListener( new ClickListener() {
                    public void clicked ( InputEvent event, float x, float y ) {
                        UIHelper.clickAnimation( interactGroup );
                        interactSystem.act();
                        if ( interactSystem.getCurrentTextBlock() != null ) {
                            showDialogMenu( interactSystem.getCurrentTextBlock() );
                        }
                    }
                } );
            }
        } else {
            readyToPick = false;
            interactGroup.clear();
            interactGroup.addAction( Actions.scaleTo( 1, 1, 0 ) );
        }
    }


    private void showInGameMenu () {
        guiType = GUI_TYPE.INGAME_MENU;

        gameGroup.clear();

        float inGameIconSize = Core.HEIGHT * 0.1f;

        final Image showIngameMenuImage = IMG.INGAME.getImageActor( inGameIconSize,
                                                                    inGameIconSize );
        showIngameMenuImage.setPosition( Core.WIDTH - inGameIconSize,
                                         Core.HEIGHT - inGameIconSize );
        gameGroup.addActor( showIngameMenuImage );

        showIngameMenuImage.addListener( new ClickListener() {
            public void clicked ( InputEvent event, float x, float y ) {
                SoundAsset.BackSound.play();
                UIHelper.clickAnimation( showIngameMenuImage );
                showGameGUI();
            }
        } );

        // scroll pane?

        int offset = (int) ( Core.HEIGHT * 0.1f );
        gameGroup.addActor( UIHelper.BlackBG() );

        Label ingameMenuTitle = UIHelper.Label( "Рюкзак", FontAsset.MONEY );
        float fontOffset = ingameMenuTitle.getHeight() * 1.5f;
        ingameMenuTitle.setPosition( offset + fontOffset,
                                     Core.HEIGHT - offset - fontOffset );
        gameGroup.addActor( ingameMenuTitle );

        Table table = new Table();
        //table.setPosition( Core.WIDTH_HALF, Core.HEIGHT_HALF );
        table.setWidth( Core.WIDTH - offset * 2 );
        //table.setHeight( Core.HEIGHT - offset * 4 );

        for ( ItemInBagg itemInBagg : MyPlayer.getBag() ) {
            Image img = itemInBagg.item.getImage( offset, offset );
            Label txt = UIHelper.Label( itemInBagg.item.getName(), FontAsset.ACTION_TEXT );
            txt.setWrap( true );
            String cntString = itemInBagg.count > 1 ? "" + itemInBagg.count : "";
            Label count = UIHelper.Label( cntString, FontAsset.ACTION_TEXT );
            Label fullTxt = UIHelper.Label( itemInBagg.item.getAbout(), FontAsset.ACTION_TEXT );
            fullTxt.setWrap( true );
            Image useImage = IMG.USABLE.getImageActor( offset, offset );

            float offset_half = offset / 2;

            table.add( img ).pad( 0, 0, 0, offset_half );
            table.add( count ).width( offset );
            table.add( txt ).width( offset * 2 );
            table.add( fullTxt ).pad( 0, offset_half, 0, 0 ).width( offset * 4 );
            table.add( useImage ).pad( 0, offset_half, 0, 0 );

            //////////////////
            table.row();

            Sprite lineSprite = GFXHelper.createSpriteRGB888( Core.WIDTH - offset * 2,
                                                              offset / 5 );
            Image lineImage = new Image( lineSprite );
            lineImage.setColor( GameColor.WHITEGL.get() );
            table.add( lineImage ).colspan( 5 ).row();
        }

        ScrollPane scrollPane = new ScrollPane( table );
        scrollPane.setPosition( offset, offset );
        scrollPane.setSize( Core.WIDTH - offset * 2, Core.HEIGHT - offset * 3.5f );
        scrollPane.setFadeScrollBars( true );
        scrollPane.setScrollingDisabled( true, false );

        gameGroup.addActor( scrollPane );
    }


    private void showDialogMenu ( TextBlock currentTextBlock ) {
        guiType = GUI_TYPE.INGAME_MENU;

        gameGroup.clear();

        final Image showIngameMenuImage = IMG.BUTTON.getImageActor( 64, 64 );
        showIngameMenuImage.setPosition( Core.WIDTH - 64, Core.HEIGHT - 64 );
        gameGroup.addActor( showIngameMenuImage );

        showIngameMenuImage.addListener( new ClickListener() {
            public void clicked ( InputEvent event, float x, float y ) {
                UIHelper.clickAnimation( showIngameMenuImage );
                showGameGUI();
            }
        } );

        ////// dialog

        int offset = (int) ( Core.HEIGHT * 0.1f );
        int w = Core.WIDTH - offset * 2;
        int h = Core.HEIGHT - offset * 2;
        Sprite bgSprite = GFXHelper.createSpriteRGB888( w, h );
        Image bgImage = new Image( bgSprite );
        bgImage.setColor( 1, 1, 1, 0.3f );
        bgImage.setPosition( offset, offset );
        gameGroup.addActor( bgImage );

        Label dialogTitle = UIHelper.Label( currentTextBlock.getTitle(), FontAsset.MONEY );
        float fontOffset = dialogTitle.getHeight() * 1.5f;
        dialogTitle.setPosition( offset + fontOffset,
                                 Core.HEIGHT - offset - fontOffset );
        gameGroup.addActor( dialogTitle );

        Label dialogBody = UIHelper.Label( currentTextBlock.getAbout(), FontAsset.ACTION_TEXT );
        dialogBody.setPosition( offset + fontOffset,
                                Core.HEIGHT - 2 * offset - fontOffset );
        dialogBody.setWrap( true );
        gameGroup.addActor( dialogBody );

        Gdx.app.debug( "connections", "" + currentTextBlock.getConnections() );

        int j = 0;
        for ( int i = 0; i < currentTextBlock.getConnections().size; i++ ) {
            final TextBlock connection = currentTextBlock.getConnections().get( i );

            Label dialogVariant = UIHelper.Label( connection.getAction(),
                                                  FontAsset.LOCATION_TITLE );
            dialogVariant.setPosition( offset + fontOffset,
                                       Core.HEIGHT_HALF - offset / 2 - i * offset );
            dialogVariant.addListener( new ClickListener() {
                public void clicked ( InputEvent event, float x, float y ) {
                    processTextBlock( connection );
                    showDialogMenu( connection );
                }
            } );
            gameGroup.addActor( dialogVariant );
            j++;
        }

        Label dialogVariant = UIHelper.Label( TextDialogAsset.CloseDialog.get(),
                                              FontAsset.LOCATION_TITLE );
        dialogVariant.setPosition( offset + fontOffset,
                                   Core.HEIGHT_HALF - offset / 2 - j * offset );
        dialogVariant.addListener( new ClickListener() {
            public void clicked ( InputEvent event, float x, float y ) {
                showGameGUI();
            }
        } );
        gameGroup.addActor( dialogVariant );
    }


    private void processTextBlock ( TextBlock connection ) {
        if ( connection.equals( TextBlock.DialogNPC4 ) ) {
            TextBlock.DialogNPC1.getConnections().clear();
            TextBlock.DialogNPC1.setText( TextDialogAsset.NPC1Name, TextDialogAsset.NPC4text,
                                          TextDialogAsset.Empty );
        }
    }


    private void showGameGUI () {
        guiType = GUI_TYPE.GAME_GUI;

        gameGroup.clear();
        touchPadGroup.clear();

        if ( touchpad == null ) {
            touchpad = UIHelper.createTouchPad();
            touchpad.setPosition( Core.HEIGHT * 0.05f, Core.HEIGHT * 0.05f );
            touchPadGroup.addActor( touchpad );
            touchPadGroup.setOrigin( Core.HEIGHT * 0.05f + Core.HEIGHT * 0.3f / 2,
                                     Core.HEIGHT * 0.05f + Core.HEIGHT * 0.3f / 2 );
        } else {
            touchpad.setPosition( Core.HEIGHT * 0.05f, Core.HEIGHT * 0.05f );
            touchPadGroup.addActor( touchpad );
            touchPadGroup.setOrigin( Core.HEIGHT * 0.05f + Core.HEIGHT * 0.3f / 2,
                                     Core.HEIGHT * 0.05f + Core.HEIGHT * 0.3f / 2 );
        }
        UIHelper.scaleIn( touchPadGroup );
        gameGroup.addActor( touchPadGroup );

        if ( jumpButton == null ) {
            jumpButton = new Image( IMG.BUTTON.createSprite() );
            jumpButton.setSize( Core.HEIGHT * 0.24f, Core.HEIGHT * 0.24f );
            jumpButton.setPosition( Core.WIDTH - jumpButton.getWidth() * 1.8f,
                                    jumpButton.getHeight() * 0.4f );
            jumpButton.setOrigin( jumpButton.getWidth() / 2,
                                  jumpButton.getHeight() / 2 );

            jumpButton.addListener( new ClickListener() {
                @Override
                public void clicked ( InputEvent event, float x, float y ) {
                    MyPlayer.startJump();
                    UIHelper.clickAnimation( jumpButton );
                }
            } );
        }
        UIHelper.scaleIn( jumpButton );
        gameGroup.addActor( jumpButton );

        float aimSize = Core.HEIGHT * 0.1f;
        Image aimImage = IMG.AIM.getImageActor( aimSize, aimSize );
        aimImage.setPosition( Core.WIDTH_HALF - aimSize / 2, Core.HEIGHT_HALF - aimSize / 2 );
        gameGroup.addActor( aimImage );

        // TODO if dead
        //player.setManagerLogLevel(); // сброс состояний, параметров и прочей фигни
        //DynamicZone.reload naxer

        if ( interactGroup != null ) {
            interactGroup.clear();
        } else {
            interactGroup = new Group();
        }
        interactGroup.setPosition( Core.WIDTH_HALF, Core.HEIGHT * 0.3f );

        gameGroup.addActor( interactGroup );

        float inGameIconSize = Core.HEIGHT * 0.16f;

        final Image showIngameMenuImage = IMG.INGAME.getImageActor( inGameIconSize,
                                                                    inGameIconSize );
        showIngameMenuImage.setPosition( Core.WIDTH - inGameIconSize,
                                         Core.HEIGHT - inGameIconSize );
        gameGroup.addActor( showIngameMenuImage );

        showIngameMenuImage.addListener( new ClickListener() {
            public void clicked ( InputEvent event, float x, float y ) {
                SoundAsset.Click.play();
                UIHelper.clickAnimation( showIngameMenuImage );
                showInGameMenu();
            }
        } );
    }


    private LoadIndicator initLoadIndicator () {
        indicatorGroup = new LoadIndicator();

        final int size = (int) ( Core.HEIGHT * 0.16f );

        Image image = new Image( TextureAsset.CD.getSprite() );
        image.setSize( size, size );
        image.setOrigin( size / 2, size / 2 );
        image.addAction( Actions.forever( Actions.sequence(
                Actions.rotateTo( 0, 0 ),
                Actions.rotateTo( 360, 1 )
                                                          ) ) );
        image.setPosition( Core.WIDTH - size * 1.5f, size / 2 );
        indicatorGroup.addActor( image );

        return indicatorGroup;
    }


    @Override
    public void backButton () {
        SoundAsset.BackSound.play();
        if ( guiType == GUI_TYPE.INGAME_MENU ) {
            showGameGUI();
        } else {
            UIHelper.scaleOut( touchPadGroup );
            UIHelper.scaleOut( jumpButton );
            transitionTo( MyGdxGame.SCREEN_TYPE.MENU );
        }
    }


    @Override
    public void dispose () {
        super.dispose();

        AshleyWorld.getPooledEngine().getSystem( NPCSystem.class ).disableWalkSound();

        MusicAsset.WINDFILTER.stop();

        jumpButton = null;
        touchpad = null;
        indicatorGroup = null;
        touchPadGroup = null;
        gameGroup = null;

        interactSystem = null;
    }


    private enum GUI_TYPE {
        GAME_GUI,
        INGAME_MENU
    }
}
