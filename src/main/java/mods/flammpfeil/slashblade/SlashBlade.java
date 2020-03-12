package mods.flammpfeil.slashblade;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import mods.flammpfeil.slashblade.ability.LockOnManager;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.imputstate.CapabilityImputState;
import mods.flammpfeil.slashblade.capability.mobeffect.CapabilityMobEffect;
import mods.flammpfeil.slashblade.capability.slashblade.CapabilitySlashBlade;
import mods.flammpfeil.slashblade.client.renderer.LockonCircleRender;
import mods.flammpfeil.slashblade.client.renderer.SlashBladeTEISR;
import mods.flammpfeil.slashblade.client.renderer.entity.JudgementCutRenderer;
import mods.flammpfeil.slashblade.client.renderer.entity.SummonedSwordRenderer;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.BladeMotionManager;
import mods.flammpfeil.slashblade.client.renderer.LayerMainBlade;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.event.*;
import mods.flammpfeil.slashblade.event.client.SneakingMotionCanceller;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import mods.flammpfeil.slashblade.item.SBItems;
import mods.flammpfeil.slashblade.network.NetworkManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SlashBlade.modid)
public class SlashBlade
{
    public static final String modid = "slashblade";

    public static final ItemGroup SLASHBLADE = new ItemGroup(modid) {
        @OnlyIn(Dist.CLIENT)
        public ItemStack createIcon() {
            ItemStack stack = new ItemStack(SBItems.slashblade);
            stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s->{
                s.setModel(new ResourceLocation(modid,"model/named/yamato.obj"));
                s.setTexture(new ResourceLocation(modid,"model/named/yamato.png"));
            });
            return stack;
        }
    };

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public SlashBlade() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading

        // Register the doClientStuff method for modloading
        DistExecutor.runWhenOn(Dist.CLIENT,()->()->{
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::Baked);
            OBJLoader.INSTANCE.addDomain("slashblade");

            MinecraftForge.EVENT_BUS.addListener(MoveImputHandler::onPlayerPostTick);
        });


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        NetworkManager.register();
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        CapabilitySlashBlade.register();
        CapabilityMobEffect.register();
        CapabilityImputState.register();

        MinecraftForge.EVENT_BUS.addListener(KnockBackHandler::onLivingKnockBack);

        FallHandler.getInstance().register();
        LockOnManager.getInstance().register();

        MinecraftForge.EVENT_BUS.register(new CapabilityAttachHandler());
        MinecraftForge.EVENT_BUS.register(new StunManager());
        AnvilCrafting.getInstance().register();
        RefineHandler.getInstance().register();
        KillCounter.getInstance().register();

        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    @OnlyIn(Dist.CLIENT)
    private void doClientStuff(final FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.register(BladeModelManager.getInstance());
        MinecraftForge.EVENT_BUS.register(BladeMotionManager.getInstance());

        Minecraft.getInstance().getRenderManager().getSkinMap().values().stream()
                .forEach((lr)->lr.addLayer(new LayerMainBlade(lr)));

        SneakingMotionCanceller.getInstance().register();
        LockonCircleRender.getInstance().register();
        BladeMaterialTooltips.getInstance().register();

        // do something that can only be done on the client

        //OBJLoader.INSTANCE.addDomain("slashblade");

        RenderingRegistry.registerEntityRenderingHandler(EntityAbstractSummonedSword.class, SummonedSwordRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityJudgementCut.class, JudgementCutRenderer::new);

        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> event) {
            // register a new block here
            IForgeRegistry<Block> registry = event.getRegistry();
            /*
            registry.register(new Block((Block.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 3.0F)))
                    .setRegistryName(modid,"material"));
                    */

            LOGGER.info("HELLO from Register Block");
        }

        static java.util.function.Supplier<java.util.concurrent.Callable<net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer>> teisr = ()->SlashBladeTEISR::new;

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> event){
            IForgeRegistry<Item> registry = event.getRegistry();
            registry.register(
                    new ItemSlashBlade(
                            new ItemTierSlashBlade(() -> {
                                Tag<Item> tags = ItemTags.getCollection().get(new ResourceLocation("slashblade","proudsouls"));
                                return Ingredient.fromTag(tags);
                                //Ingredient.fromItems(SBItems.proudsoul)
                            }),
                            1,
                            -2.4F,
                            (new Item.Properties()).group(ItemGroup.COMBAT)
                                   .setTEISR(teisr)) /*()->SlashBladeTEISR::new*/
                            .setRegistryName(modid,"slashblade"));

            ToolType proudsoulLevel = ToolType.get("proudsoul");

            registry.register(
                    new Item((new Item.Properties()).group(SLASHBLADE).addToolType(proudsoulLevel,2)){
                        @Override
                        public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {

                            CompoundNBT tag = entity.serializeNBT();
                            tag.putInt("Health", 50);
                            int age = tag.getShort("Age");
                            entity.deserializeNBT(tag);

                            if(entity.isGlowing()){
                                entity.setMotion(entity.getMotion().mul(0.8,0.0,0.8).add(0.0D, +0.04D, 0.0D));
                            }else if(entity.isBurning()) {
                                entity.setMotion(entity.getMotion().mul(0.8,0.5,0.8).add(0.0D, +0.04D, 0.0D));
                            }

                            return false;
                        }

                        @Override
                        public boolean hasEffect(ItemStack stack) {
                            return true;//super.hasEffect(stack);
                        }
                    }.setRegistryName(modid,"proudsoul"));

            registry.register(
                    new Item((new Item.Properties()).group(SLASHBLADE).addToolType(proudsoulLevel,3)){
                        @Override
                        public boolean hasEffect(ItemStack stack) {
                            return true;//super.hasEffect(stack);
                        }
                    }.setRegistryName(modid,"proudsoul_ingot"));

            registry.register(
                    new Item((new Item.Properties()).group(SLASHBLADE).addToolType(proudsoulLevel,1)){
                        @Override
                        public boolean hasEffect(ItemStack stack) {
                            return true;//super.hasEffect(stack);
                        }
                    }.setRegistryName(modid,"proudsoul_tiny"));

            registry.register(
                    new Item((new Item.Properties()).group(SLASHBLADE).addToolType(proudsoulLevel,4).rarity(Rarity.UNCOMMON)){
                        @Override
                        public boolean hasEffect(ItemStack stack) {
                            return true;//super.hasEffect(stack);
                        }
                    }.setRegistryName(modid,"proudsoul_sphere"));

            registry.register(
                    new Item((new Item.Properties()).group(SLASHBLADE).addToolType(proudsoulLevel,5).rarity(Rarity.RARE)){
                        @Override
                        public boolean hasEffect(ItemStack stack) {
                            return true;//super.hasEffect(stack);
                        }
                    }.setRegistryName(modid,"proudsoul_crystal"));

            registry.register(
                    new Item((new Item.Properties()).group(SLASHBLADE).addToolType(proudsoulLevel,6).rarity(Rarity.EPIC)){
                        @Override
                        public boolean hasEffect(ItemStack stack) {
                            return true;//super.hasEffect(stack);
                        }
                    }.setRegistryName(modid,"proudsoul_trapezohedron"));
        }

        public static final ResourceLocation SummonedSwordLoc = new ResourceLocation(SlashBlade.modid, classToString(EntityAbstractSummonedSword.class));
        public static final EntityType<EntityAbstractSummonedSword> SummonedSword = EntityType.Builder
                .create(EntityAbstractSummonedSword::new, EntityClassification.MISC)
                .size(0.5F, 0.5F)
                .setTrackingRange(64)
                .setUpdateInterval(1)
                .setCustomClientFactory(EntityAbstractSummonedSword::createInstance)
                .build(SummonedSwordLoc.toString());

        public static final ResourceLocation JudgementCutLoc = new ResourceLocation(SlashBlade.modid, classToString(EntityJudgementCut.class));
        public static final EntityType<EntityJudgementCut> JudgementCut = EntityType.Builder
                .create(EntityJudgementCut::new, EntityClassification.MISC)
                .size(2.5F, 2.5F)
                .setTrackingRange(64)
                .setUpdateInterval(1)
                .setCustomClientFactory(EntityJudgementCut::createInstance)
                .build(JudgementCutLoc.toString());

        @SubscribeEvent
        public static void onEntitiesRegistry(final RegistryEvent.Register<EntityType<?>> event){
            {
                EntityType<EntityAbstractSummonedSword> entity = SummonedSword;
                entity.setRegistryName(SummonedSwordLoc);
                event.getRegistry().register(entity);
            }

            {
                EntityType<EntityJudgementCut> entity = JudgementCut;
                entity.setRegistryName(JudgementCutLoc);
                event.getRegistry().register(entity);
            }
        }

        private static String classToString(Class<? extends Entity> entityClass) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName()).replace("entity_", "");
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void Baked(final ModelBakeEvent event){
        {
            ModelResourceLocation loc = new ModelResourceLocation(
                    ForgeRegistries.ITEMS.getKey(SBItems.slashblade), "inventory");
            BladeModel model = new BladeModel(event.getModelRegistry().get(loc), event.getModelLoader());
            event.getModelRegistry().put(loc, model);
        }

        overrideModel(event, SBItems.proudsoul, new ResourceLocation(modid, "block/soul.obj"));
        overrideModel(event, SBItems.proudsoul_ingot, new ResourceLocation(modid, "block/ingot.obj"));
        overrideModel(event, SBItems.proudsoul_tiny, new ResourceLocation(modid, "block/tiny.obj"));
        overrideModel(event, SBItems.proudsoul_sphere, new ResourceLocation(modid, "block/sphere.obj"));
        overrideModel(event, SBItems.proudsoul_crystal, new ResourceLocation(modid, "block/crystal.obj"));
        overrideModel(event, SBItems.proudsoul_trapezohedron, new ResourceLocation(modid, "block/trapezohedron.obj"));
    }

    @OnlyIn(Dist.CLIENT)
    private void overrideModel(final ModelBakeEvent event, Item item, ResourceLocation newLoc){
        ModelResourceLocation loc = new ModelResourceLocation(
                ForgeRegistries.ITEMS.getKey(item), "inventory");


        EnumMap<ItemCameraTransforms.TransformType, TRSRTransformation> block = new EnumMap<>(ItemCameraTransforms.TransformType.class);
        TRSRTransformation thirdPersonBlock = ForgeBlockStateV1.Transforms.convert(0, 2.5f, 0, 75, 45, 0, 0.375f);
        block.put(ItemCameraTransforms.TransformType.GUI,                      ForgeBlockStateV1.Transforms.convert(0, 0, 0, 10, 0, 0, 0.9f));
        block.put(ItemCameraTransforms.TransformType.GROUND,                  ForgeBlockStateV1.Transforms.convert(0, 3, 0, 0, 0, 0, 0.5f));
        block.put(ItemCameraTransforms.TransformType.FIXED,                   ForgeBlockStateV1.Transforms.convert(0, 0, -10, -90, 0, 0, 1.0f));
        block.put(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, thirdPersonBlock);
        block.put(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND,  ForgeBlockStateV1.Transforms.leftify(thirdPersonBlock));
        block.put(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ForgeBlockStateV1.Transforms.convert(0, 0, 0, 0, 45, 0, 0.4f));
        block.put(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,  ForgeBlockStateV1.Transforms.convert(0, 0, 0, 0, 225, 0, 0.4f));
        SimpleModelState state = new SimpleModelState(ImmutableMap.copyOf(block));

        IUnbakedModel unbaked = ModelLoaderRegistry.getModelOrMissing(newLoc);
        event.getModelRegistry().put(loc , unbaked.bake(event.getModelLoader(), ModelLoader.defaultTextureGetter(), (ISprite) state, DefaultVertexFormats.ITEM));
    }

}