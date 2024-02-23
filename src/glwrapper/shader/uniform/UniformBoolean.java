package glwrapper.shader.uniform;

import org.lwjgl.opengl.GL20;

public class UniformBoolean extends Uniform {

	private boolean currentBool;
	private final boolean defaultValue;
	
	public UniformBoolean(String name, boolean defaultValue){
		super(name);
		this.defaultValue = defaultValue;
	}

	@Override
	public Boolean getDefault() {
		return defaultValue;
	}

	public void loadDefault() {
		loadBoolean(defaultValue);
	}

	@Override
	public void accept(UniformVisitor visitor) {
		visitor.visit(this);
	}

	public void loadBoolean(boolean bool){
		if(currentBool != bool){
			GL20.glUniform1i(super.getLocation(), bool ? 1 : 0);
			currentBool = bool;
		}
	}
}
