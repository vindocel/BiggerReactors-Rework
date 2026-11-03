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
    int light() default 0;
    
    /** Added 2026-11-03: entities inside the placed fluid freeze as in powder snow. */
    boolean freezes() default false;
    
    /** Added 2026-11-03: entities inside the placed fluid burn as in lava. */
    boolean burns() default false;
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface Instance {
    }
}
