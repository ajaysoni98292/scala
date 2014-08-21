package com.ecsc.simple.game

import java.awt._;
import java.awt.geom._;
import java.awt.image._;
import java.awt.event._;
import javax.swing._;
import java.util._;

object SpaceInvaders {
val w = 640;
val H = 480;

def main(args : Array[String]) {
  		val frame = new JFrame() {
			{
				setSize(w, H);
				setTitle("Space Invaders");
				setContentPane(new JPanel() {
					@Override
					def paintComponent( g : Graphics) {
						val img = SpaceInvaders.currentState.render().backingImage;
						((Graphics2D) g).drawRenderedImage(img,
								new AffineTransform());
					}
				});
				addKeyListener(new KeyAdapter() {
					@Override
					def keyPressed( e : KeyEvent) {
						currentState = currentState.keyPressed(e.getKeyCode());
					}

					@Override
					def keyReleased(e : KeyEvent) {
						currentState = currentState.keyReleased(e.getKeyCode());
					}
				});
				setLocationByPlatform(true);
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				setVisible(true);
			}
		};

		for (;;) {
			currentState = currentState.update();
			frame.repaint();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
		}
}

}

class GameState {
  	val Player player;
	val ImmutableSet<Bullet> bullets;
	val KeyboardState keyboard;

	def GameState(player : Player,  bullets :ImmutableSet<Bullet> ,
			 keyboard :KeyboardState) {
		this.player = player;
		this.bullets = bullets;
		this.keyboard = keyboard;
	}

	GameState() {
		this(new Player(), new ImmutableSet<Bullet>(), new KeyboardState());
	}

	GameState setPlayer(Player newPlayer) {
		return new GameState(newPlayer, bullets, keyboard);
	}

	GameState setBullets(ImmutableSet<Bullet> newBullets) {
		return new GameState(player, newBullets, keyboard);
	}

	GameState setKeyboard(KeyboardState newKeyboard) {
		return new GameState(player, bullets, newKeyboard);
	}

	// Update the game state (repeatedly called for each game tick).
	GameState update() {
		GameState current = this;
		current = current.player.update(current);
		for (Bullet b : current.bullets)
			current = b.update(current);
		return current;
	}

	// Update the game state in response to a key press.
	GameState keyPressed(int key) {
		GameState current = this;
		current = keyboard.keyPressed(current, key);
		current = player.keyPressed(current, key);
		return current;
	}

	// Update the game state in response to a key release.
	GameState keyReleased(int key) {
		GameState current = this;
		current = keyboard.keyReleased(current, key);
		return current;
	}

	ImmutableImage render() {
		ImmutableImage img = new ImmutableImage(SpaceInvaders.W,
				SpaceInvaders.H);
		img = img.clear(Color.BLUE);
		img = player.render(img);
		for (Bullet b : bullets)
			img = b.render(img);
		return img;
	}

}

class ImmutableImage {

	// Construct a blank image.
	 def ImmutableImage( w : Int , H : Int ) {
		val backingImage = new BufferedImage(w, H, BufferedImage.TYPE_INT_RGB);
	}

	// Copy constructor.
	def ImmutableImage(src :ImmutableImage ) {
		val backingImage = new BufferedImage(src.backingImage.getColorModel(),
				src.backingImage.copyData(null), false, null);
	}

	// Clear the image.
	def ImmutableImage clear( c :Color) {
		ImmutableImage copy = new ImmutableImage(this);
		Graphics g = copy.backingImage.getGraphics();
		g.setColor(c);
		g.fillRect(0, 0, backingImage.getWidth(), backingImage.getHeight());
		return copy;
	}

	// Draw a filled circle.
	 def fillCircle(x : Int , y : Int, r : Int, c : Color ) :ImmutableImage {
		ImmutableImage copy = new ImmutableImage(this);
		Graphics g = copy.backingImage.getGraphics();
		g.setColor(c);
		g.fillOval(x - r, y - r, r * 2, r * 2);
		return copy;
	}
}

// An immutable, copy-on-write object describing the player.
class Player {
	final int x, y;
	final int ticksUntilFire;

	Player(int x, int y, int ticksUntilFire) {
		this.x = x;
		this.y = y;
		this.ticksUntilFire = ticksUntilFire;
	}

	// Construct a player at the starting position, ready to fire.
	Player() {
		this(SpaceInvaders.w / 2, SpaceInvaders.H - 50, 0);
	}

	// Update the game state (repeatedly called for each game tick).
	def update( currentState :GameState) : GameState {
		// Update the player's position  based on which keys are down.
		Int newX = x;
		if (currentState.keyboard.isDown(VK_LEFT)
				|| currentState.keyboard.isDown(VK_A))
			newX -= 2;
		if (currentState.keyboard.isDown(VK_RIGHT)
				|| currentState.keyboard.isDown(VK_D))
			newX += 2;

		// Update the time until the player can fire.
		int newTicksUntilFire = ticksUntilFire;
		if (newTicksUntilFire > 0)
			--newTicksUntilFire;

		// Replace the old player with an updated player.
		Player newPlayer = new Player(newX, y, newTicksUntilFire);
		return currentState.setPlayer(newPlayer);
	}

	// Update the game state in response to a key press.
	def keyPressed(currentState : GameState , key : Int) :GameState  {
		if (key == VK_SPACE && ticksUntilFire == 0) {
			// Fire a bullet.
			Bullet b = new Bullet(x, y);
			ImmutableSet<Bullet> newBullets = currentState.bullets.plus(b);
			currentState = currentState.setBullets(newBullets);

			// Make the player wait 25 ticks before firing again.
			currentState = currentState.setPlayer(new Player(x, y, 25));
		}
		return currentState;
	}

	 def render(img : ImmutableImage ) : ImmutableImage{
		return img.fillCircle(x, y, 20, Color.RED);
	}
}

// An immutable, copy-on-write object describing a bullet.
class Bullet {
	val radius = 5;
	val x ,y;
	Bullet(x : Int =x , y : Int =y) {
		this.x = x;
		this.y = y;
	}

	// Update the game state (repeatedly called for each game tick).
	 def update(currentState :GameState ) :GameState {
		ImmutableSet<Bullet> bullets = currentState.bullets;
		bullets = bullets.minus(this);
		if (y + radius >= 0)
			// Add a copy of the bullet which has moved up the screen slightly.
			bullets = bullets.plus(new Bullet(x, y - 5));
		return currentState.setBullets(bullets);
	}

	 def render( img :ImmutableImage): ImmutableImage {
		return img.fillCircle(x, y, radius, Color.BLACK);
	}
}

// An immutable, copy-on-write snapshot of the keyboard state at some time.
class KeyboardState {


	def KeyboardState( depressedKeys : Integer) {
		this.depressedKeys = depressedKeys;
	}

	 def keyPressed(currentState : GameState , key : Int) :GameState {
		return currentState.setKeyboard(new KeyboardState(depressedKeys
				.plus(key)));
	}

	 def keyReleased(currentState : GameState , key : Int) : GameState {
		return currentState.setKeyboard(new KeyboardState(depressedKeys
				.minus(key)));
	}

 def isDown(key :Int) : Boolean {
		return depressedKeys.contains(key);
	}
}