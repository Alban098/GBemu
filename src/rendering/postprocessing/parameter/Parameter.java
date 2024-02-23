package rendering.postprocessing.parameter;

import org.joml.*;

import java.util.Locale;

public abstract class Parameter {

    private final String name;

    public Parameter(String name) {
        this.name = name;
    }

    public abstract Object getValue();

    public String getName() {
        return name;
    }

    public abstract String getType();

    public abstract void accept(ParameterVisitor visitor);

    public static Parameter get(String name, String type, String value) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "boolean" -> {
                return new BooleanParameter(name, Boolean.parseBoolean(value));
            }
            case "integer" -> {
                return new IntegerParameter(name, Integer.parseInt(value));
            }
            case "float" -> {
                return new FloatParameter(name, Float.parseFloat(value));
            }
            case "vec2" -> {
                String[] parsed = value.replaceAll(" ", "").split(";");
                Vector2f vec2;
                if (parsed.length != 2) {
                    vec2 = new Vector2f();
                } else {
                    vec2 = new Vector2f(
                            Float.parseFloat(parsed[0]),
                            Float.parseFloat(parsed[1])
                    );
                }
                return new Vec2Parameter(name, vec2);
            }
            case "vec3" -> {
                String[] parsed = value.replaceAll(" ", "").split(";");
                Vector3f vec3;
                if (parsed.length != 3) {
                    vec3 = new Vector3f();
                } else {
                    vec3 = new Vector3f(
                            Float.parseFloat(parsed[0]),
                            Float.parseFloat(parsed[1]),
                            Float.parseFloat(parsed[2])
                    );
                }
                return new Vec3Parameter(name, vec3);
            }
            case "vec4" -> {
                String[] parsed = value.replaceAll(" ", "").split(";");
                Vector4f vec4;
                if (parsed.length != 4) {
                    vec4 = new Vector4f();
                } else {
                    vec4 = new Vector4f(
                            Float.parseFloat(parsed[0]),
                            Float.parseFloat(parsed[1]),
                            Float.parseFloat(parsed[2]),
                            Float.parseFloat(parsed[3])
                    );
                }
                return new Vec4Parameter(name, vec4);
            }
            case "mat2" -> {
                String[] parsed = value.replaceAll(" ", "").split(";");
                Matrix2f mat2;
                if (parsed.length != 4) {
                    mat2 = new Matrix2f();
                } else {
                    mat2 = new Matrix2f(
                            Float.parseFloat(parsed[0]),
                            Float.parseFloat(parsed[1]),
                            Float.parseFloat(parsed[2]),
                            Float.parseFloat(parsed[3])
                    );
                }
                return new Mat2Parameter(name, mat2);
            }
            case "mat3" -> {
                String[] parsed = value.replaceAll(" ", "").split(";");
                Matrix3f mat3;
                if (parsed.length != 9) {
                    mat3 = new Matrix3f();
                } else {
                    mat3 = new Matrix3f(
                            Float.parseFloat(parsed[0]),
                            Float.parseFloat(parsed[1]),
                            Float.parseFloat(parsed[2]),
                            Float.parseFloat(parsed[3]),
                            Float.parseFloat(parsed[4]),
                            Float.parseFloat(parsed[8]),
                            Float.parseFloat(parsed[6]),
                            Float.parseFloat(parsed[7]),
                            Float.parseFloat(parsed[8])
                    );
                }
                return new Mat3Parameter(name, mat3);
            }
            case "mat4" ->  {
                String[] parsed = value.replaceAll(" ", "").split(";");
                Matrix4f mat4;
                if (parsed.length != 16) {
                    mat4 = new Matrix4f();
                } else {
                    mat4 = new Matrix4f(
                            Float.parseFloat(parsed[0]),
                            Float.parseFloat(parsed[1]),
                            Float.parseFloat(parsed[2]),
                            Float.parseFloat(parsed[3]),
                            Float.parseFloat(parsed[4]),
                            Float.parseFloat(parsed[8]),
                            Float.parseFloat(parsed[6]),
                            Float.parseFloat(parsed[7]),
                            Float.parseFloat(parsed[8]),
                            Float.parseFloat(parsed[9]),
                            Float.parseFloat(parsed[10]),
                            Float.parseFloat(parsed[11]),
                            Float.parseFloat(parsed[12]),
                            Float.parseFloat(parsed[13]),
                            Float.parseFloat(parsed[14]),
                            Float.parseFloat(parsed[15])
                    );
                }
                return new Mat4Parameter(name, mat4);
            }
            default -> {
                return null;
            }
        }
    }

    public abstract Class<?> getUniformClass();
}
