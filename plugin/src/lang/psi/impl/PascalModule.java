package com.siberika.idea.pascal.lang.psi.impl;

import com.siberika.idea.pascal.lang.psi.PasEntityScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Author: George Bakhtadze
 * Date: 13/08/2015
 */
public interface PascalModule extends PasEntityScope {

    enum ModuleType {
        UNIT, PROGRAM, LIBRARY, PACKAGE
    }

    ModuleType getModuleType();

    @Nullable
    PasField getPublicField(final String name);

    @Nullable
    PasField getPrivateField(final String name);

    @NotNull
    Collection<PasField> getPrivateFields();

    @NotNull
    Collection<PasField> getPubicFields();

    List<PasEntityScope> getPrivateUnits();

    List<PasEntityScope> getPublicUnits();

}
