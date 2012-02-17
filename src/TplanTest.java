import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TplanTest {
	public String comparisonMethod() default "search";
	public String waitFor() default "1s";
	public float matchRate() default 95F;
	public String matchArea() default "desktop";
	public boolean debug() default false;
}