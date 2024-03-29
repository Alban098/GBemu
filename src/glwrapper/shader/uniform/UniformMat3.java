package glwrapper.shader.uniform;

import org.joml.Matrix3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class UniformMat3 extends Uniform{

	private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(9);
	private final Matrix3f defaultValue;

	public UniformMat3(String name, Matrix3f defaultValue) {
		super(name);
		this.defaultValue = defaultValue;
	}

	@Override
	public Matrix3f getDefault() {
		return defaultValue;
	}

	public void loadDefault() {
		loadMatrix(defaultValue);
	}

	public void loadMatrix(Matrix3f matrix){
		matrix.get(matrixBuffer);
		matrixBuffer.flip();
		GL20.glUniformMatrix3fv(super.getLocation(), false, matrixBuffer);
	}

	@Override
	public void accept(UniformVisitor visitor) {
		visitor.visit(this);
	}
}
