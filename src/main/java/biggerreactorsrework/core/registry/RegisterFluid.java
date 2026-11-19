package biggerreactorsrework.core.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterFluid {

    String modid() default "";

    String name();

    boolean registerBucket() default false;

    boolean creativeTab() default true;

    int color() default 0xFFFFFFFF;

    /** Added 2026-11-03: light emitted by the fluid where it is drawn. */
//? if =26.1.2 || =26.3 {
    int light() default 0;

    /** Use water movement and swimming for entities in the placed fluid. */
    boolean waterLikeMovement() default false;
//?} else if =1.21.1 {
/*    int light() default 0;

    /^* Use water movement for entities in the placed fluid, including landing without fall damage. ^/
    boolean waterLikeMovement() default false;
*/
//?}

    /** Added 2026-11-03: entities inside the placed fluid freeze as in powder snow. */
//? if =26.1.2 || =26.3 {
    boolean freezes() default false;

    /** Added 2026-11-03: entities inside the placed fluid burn as in lava. */
    boolean burns() default false;

    @Retention(RetentionPolicy.RUNTIME)
//?} else if =1.21.1 {
/*    boolean freezes() default false;

    /^* Added 2026-11-03: entities inside the placed fluid burn as in lava. ^/
    boolean burns() default false;

    @Retention(RetentionPolicy.RUNTIME)
*/
//?}
    @interface Instance {
    }
}
