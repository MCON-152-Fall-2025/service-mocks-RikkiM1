package com.mcon152.recipeshare.service;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.repository.RecipeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Assignment: Implement all TODOs using Mockito features covered in class:
 *  - @Mock, @InjectMocks, @Captor, @ExtendWith(MockitoExtension.class)
 *  - Stubbing: thenReturn / thenAnswer / thenThrow
 *  - Verifications: verify(...), times/never/atLeast..., verifyNoMoreInteractions
 *  - InOrder (where meaningful)
 *  - Void stubbing: doNothing / doThrow (use deleteById for this)
 *  - Matchers: any(), eq(), argThat()
 *  - ArgumentCaptor
 *  - (Optional) Spy demo if you introduce a small helper in tests
 *
 * NOTE: This is a pure unit test. Do NOT start a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService (Mockito) — Assignment Skeleton")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService; // CUT implements RecipeService

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    // --- Helpers for sample data ---

    private Recipe newRecipeNoId() {
        return new Recipe(
                null,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    private Recipe savedRecipe(long id) {
        return new Recipe(
                id,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    // ------------------ addRecipe ------------------

    @Nested
    @DisplayName("addRecipe(Recipe)")
    class AddRecipe {

        @Test
        @DisplayName("returns saved entity (thenReturn) and calls repository.save once")
        void returnsSaved_andSavesOnce() {
            // arrange
            Recipe input = newRecipeNoId();
            Recipe saved = savedRecipe(1L);

            when(recipeRepository.save(any(Recipe.class)))
                    .thenReturn(saved);

            // act
            Recipe out = recipeService.addRecipe(input);

            // assert
            assertNotNull(out.getId());
            assertEquals(1L, out.getId());
            assertEquals(saved, out);

            // verify
            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }
        @Test
        @DisplayName("assigns ID dynamically (thenAnswer) and captures argument")
        void assignsId_thenAnswer_andCaptures() {
            // arrange
            ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);

            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
                Recipe r = inv.getArgument(0);
                return new Recipe(
                        1L,
                        r.getTitle(),
                        r.getDescription(),
                        r.getIngredients(),
                        r.getInstructions(),
                        r.getServings()
                );
            });

            // act
            Recipe out = recipeService.addRecipe(newRecipeNoId());

            // assert (returned value)
            assertEquals(1L, out.getId());

            // assert (captured argument)
            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertNull(sent.getId()); // before persistence
            assertEquals("Chocolate Cake", sent.getTitle());
        }

        @Test
        @DisplayName("propagates repository failure (thenThrow)")
        void propagatesRepositoryFailure() {
            // arrange
            when(recipeRepository.save(any()))
                    .thenThrow(new IllegalStateException("DB down"));

            // act + assert
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> recipeService.addRecipe(newRecipeNoId())
            );

            assertEquals("DB down", ex.getMessage());

            // verify
            verify(recipeRepository).save(any());
        }

    // ------------------ getAllRecipes ------------------

    @Nested
    @DisplayName("getAllRecipes()")
    class GetAllRecipes {

        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            // arrange
            List<Recipe> recipes = List.of(
                    new Recipe(1L, "Pasta"),
                    new Recipe(2L, "Soup")
            );

            when(recipeRepository.findAll()).thenReturn(recipes);

            // act
            List<Recipe> result = recipeService.getAllRecipes();

            // assert
            assertEquals(recipes.size(), result.size());
            assertEquals(recipes, result);
            verify(recipeRepository).findAll();
        }
    }

    // ------------------ getRecipeById ------------------

    @Nested
    @DisplayName("getRecipeById(long)")
    class GetById {

        @Test
        @DisplayName("returns Optional.present when found")
        void present() {
            // arrange
            Recipe savedRecipe = new Recipe();
            savedRecipe.setId(1L);

            when(recipeRepository.findById(1L))
                    .thenReturn(Optional.of(savedRecipe));

            // act
            Optional<Recipe> result = recipeService.findById(1L);

            // assert
            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
        }

        @Test
        @DisplayName("returns Optional.empty when missing")
        void empty() {
            // TODO: stub Optional.empty, assert empty

        }
    }

    // ------------------ deleteRecipe ------------------

    @Nested
    @DisplayName("deleteRecipe(long)")
    class DeleteRecipe {

        @Test
        @DisplayName("returns true when entity existed")
        void returnsTrue_whenExists() {
            // arrange
            long id = 1L;

            when(recipeRepository.existsById(id)).thenReturn(true);
            doNothing().when(recipeRepository).deleteById(id);

            InOrder inOrder = inOrder(recipeRepository);

            // act
            boolean result = recipeService.deleteRecipe(id);

            // assert
            assertTrue(result);

            // verify call order
            inOrder.verify(recipeRepository).existsById(id);
            inOrder.verify(recipeRepository).deleteById(id);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    @DisplayName("returns false when missing (never deletes)")
    void returnsFalse_whenMissing() {
        // arrange
        long id = 1L;
        when(recipeRepository.existsById(id)).thenReturn(false);

        // act
        boolean result = recipeService.deleteRecipe(id);

        // assert
        assertFalse(result);

        // verify
        verify(recipeRepository).existsById(id);
        verify(recipeRepository, never()).deleteById(id);
        verifyNoMoreInteractions(recipeRepository);
    }

    @Test
    @DisplayName("propagates delete error (doThrow)")
    void propagatesDeleteError() {
        // arrange
        long id = 1L;
        when(recipeRepository.existsById(id)).thenReturn(true);
        doThrow(new IllegalStateException("DB down")).when(recipeRepository).deleteById(id);

        // act + assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> recipeService.deleteRecipe(id)
        );
        assertEquals("DB down", ex.getMessage());

        // verify
        verify(recipeRepository).existsById(id);
        verify(recipeRepository).deleteById(id);
        verifyNoMoreInteractions(recipeRepository);
    }
    }

    // ------------------ updateRecipe ------------------

@Nested
@DisplayName("updateRecipe(long, Recipe)")
class UpdateRecipe {

    @Test
    @DisplayName("returns updated entity when exists")
    void returnsUpdated_whenExists() {
        // arrange
        long id = 1L;
        Recipe existing = new Recipe(id, "Old Cake", "Old desc", null, null, 2);
        Recipe update = new Recipe(null, "New Cake", "New desc", null, null, 4);
        Recipe updatedSaved = new Recipe(id, "New Cake", "New desc", null, null, 4);

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);

        when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(updatedSaved);

        // act
        Optional<Recipe> result = recipeService.updateRecipe(id, update);

        // assert
        assertTrue(result.isPresent());
        assertEquals(updatedSaved.getTitle(), result.get().getTitle());
        assertEquals(updatedSaved.getDescription(), result.get().getDescription());
        assertEquals(id, result.get().getId());

        // capture and verify saved entity
        verify(recipeRepository).save(captor.capture());
        Recipe savedArg = captor.getValue();
        assertEquals("New Cake", savedArg.getTitle());
        assertEquals("New desc", savedArg.getDescription());
        assertEquals(id, savedArg.getId());

        // verify repository interactions
        verify(recipeRepository).findById(id);
        verifyNoMoreInteractions(recipeRepository);
    }
}
@Test
@DisplayName("returns empty when entity missing")
void returnsEmpty_whenMissing() {
    // given
    when(recipeRepository.findById(1L))
            .thenReturn(Optional.empty());

    // when
    Optional<Recipe> result =
            recipeService.patchRecipe(1L, new Recipe());

    // then
    assertTrue(result.isEmpty());
    verify(recipeRepository, never()).save(any());
}

    // ------------------ patchRecipe ------------------

@Nested
@DisplayName("patchRecipe(long, Recipe)")
class PatchRecipe {

    @Test
    @DisplayName("applies only non-null fields (argThat)")
    void appliesNonNullFields_only() {
        // given
        Recipe existing = new Recipe();
        existing.setId(1L);
        existing.setTitle("Old Title");
        existing.setDescription("Original description");
        existing.setServings(4);

        Recipe partial = new Recipe();
        partial.setTitle("New Title"); // ONLY field being patched

        when(recipeRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        // echo back the saved entity
        when(recipeRepository.save(any(Recipe.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
//h
        // when
        Optional<Recipe> result = recipeService.patchRecipe(1L, partial);

        // then
        assertTrue(result.isPresent());
        assertEquals("New Title", result.get().getTitle());
        assertEquals("Original description", result.get().getDescription());
        assertEquals(4, result.get().getServings());

        verify(recipeRepository).save(argThat(saved ->
                saved.getTitle().equals("New Title") &&
                        saved.getDescription().equals("Original description") &&
                        saved.getServings() == 4
        ));
    }

    @Test
    @DisplayName("returns empty when entity missing")
    void returnsEmpty_whenMissing() {
        // given
        when(recipeRepository.findById(99L))
                .thenReturn(Optional.empty());

        // when
        Optional<Recipe> result =
                recipeService.patchRecipe(99L, new Recipe());

        // then
        assertTrue(result.isEmpty());
        verify(recipeRepository, never()).save(any());
    }
}

    // ------------------ extra practice ------------------

    @Nested
    @DisplayName("Advanced stubbing & verification")
    class Advanced {

        @Test
        @DisplayName("consecutive stubs on existsById (true, false)")
        void consecutiveStubs_existsById() {
            // TODO: when(existsById(1L)).thenReturn(true, false); verify two calls and no more

         }
    }
}
