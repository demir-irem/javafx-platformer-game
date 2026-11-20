public class Apple extends GameObject{
    public Apple(){
        super("assets/Apple(16x18x17).png",16,18);
    }
    @Override
    public void update(){
        animate(17,4);
    }
}
